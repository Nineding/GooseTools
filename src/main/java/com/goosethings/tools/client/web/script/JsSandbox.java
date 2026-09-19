package com.goosethings.tools.client.web.script;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.web.dom.DomElement;
import com.goosethings.tools.client.web.dom.WebDocument;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Rhino safe-standard-objects scope with only a narrow DOM bridge. */
public final class JsSandbox implements AutoCloseable {
    private static final long MAX_EXECUTION_NANOS = 75_000_000L;
    private static final long MAX_EVENT_EXECUTION_NANOS = 250_000_000L;
    private static final int INSTRUCTION_THRESHOLD = 10_000;

    private final SafeContextFactory contextFactory = new SafeContextFactory();
    private final WebDocument document;
    private final Consumer<String> pageNavigator;
    private final Map<DomElement, Scriptable> wrappers = new IdentityHashMap<>();
    private final Map<Scriptable, DomElement> reverseWrappers = new IdentityHashMap<>();
    private Scriptable scope;
    private boolean closed;

    public JsSandbox(WebDocument document, Consumer<String> pageNavigator) {
        this.document = document;
        this.pageNavigator = pageNavigator;
        run(context -> {
            scope = context.initSafeStandardObjects(null, false);
            ScriptableObject.deleteProperty(scope, "Packages");
            ScriptableObject.deleteProperty(scope, "java");
            ScriptableObject.deleteProperty(scope, "javax");
            ScriptableObject.deleteProperty(scope, "org");
            ScriptableObject.deleteProperty(scope, "com");
            ScriptableObject.deleteProperty(scope, "eval");
            ScriptableObject.deleteProperty(scope, "Function");
            ScriptableObject.putProperty(scope, "document", createDocumentObject(context));
            ScriptableObject.putProperty(scope, "console", createConsoleObject());
            ScriptableObject.putProperty(scope, "goose", createGooseObject());
            return null;
        });
    }

    public void executeScripts() {
        for (WebDocument.ScriptSource script : document.scripts()) {
            try {
                run(context -> {
                    context.evaluateString(scope, script.source(), script.name(), 1, null);
                    return null;
                });
            } catch (RuntimeException exception) {
                GooseTools.LOGGER.warn("Blocked or failed GooseTools web script {}: {}", script.name(), exception.getMessage());
            }
        }
    }

    public void fire(DomElement element, String eventName) {
        if (closed) {
            return;
        }
        List<Function> handlers = element.eventHandlers(eventName);
        if (handlers.isEmpty()) {
            return;
        }
        try {
            run(MAX_EVENT_EXECUTION_NANOS, context -> {
                Scriptable target = wrapElement(context, element);
                NativeObject event = nativeObject();
                ScriptableObject.putProperty(event, "type", eventName);
                ScriptableObject.putProperty(event, "target", target);
                for (Function handler : handlers) {
                    handler.call(context, scope, target, new Object[]{event});
                }
                return null;
            });
        } catch (RuntimeException exception) {
            GooseTools.LOGGER.warn("Blocked or failed GooseTools web {} handler: {}", eventName, exception.getMessage());
        }
    }

    private Scriptable createDocumentObject(Context context) {
        NativeObject object = nativeObject();
        putFunction(object, "getElementById", args -> {
            DomElement element = document.getElementById(stringArg(args, 0));
            return element == null ? null : wrapElement(Context.getCurrentContext(), element);
        });
        putFunction(object, "querySelector", args -> {
            List<DomElement> elements = document.querySelectorAll(stringArg(args, 0));
            return elements.isEmpty() ? null : wrapElement(Context.getCurrentContext(), elements.getFirst());
        });
        putFunction(object, "querySelectorAll", args -> {
            Object[] elements = document.querySelectorAll(stringArg(args, 0)).stream()
                    .map(element -> wrapElement(Context.getCurrentContext(), element))
                    .toArray();
            return Context.getCurrentContext().newArray(scope, elements);
        });
        putFunction(object, "createElement", args -> wrapElement(
                Context.getCurrentContext(), document.createElement(stringArg(args, 0))));
        ScriptableObject.putProperty(object, "body", wrapElement(context, document.root()));
        return object;
    }

    private Scriptable createConsoleObject() {
        NativeObject object = nativeObject();
        putFunction(object, "log", args -> {
            GooseTools.LOGGER.info("[WebUI] {}", joinArgs(args));
            return Undefined.instance;
        });
        putFunction(object, "warn", args -> {
            GooseTools.LOGGER.warn("[WebUI] {}", joinArgs(args));
            return Undefined.instance;
        });
        putFunction(object, "error", args -> {
            GooseTools.LOGGER.error("[WebUI] {}", joinArgs(args));
            return Undefined.instance;
        });
        return object;
    }

    private Scriptable createGooseObject() {
        NativeObject object = nativeObject();
        putFunction(object, "openPage", args -> {
            pageNavigator.accept(stringArg(args, 0));
            return Undefined.instance;
        });
        putFunction(object, "setLanguage", args -> document.setLanguage(stringArg(args, 0)));
        putFunction(object, "getLanguage", args -> document.translations().activeCode());
        return object;
    }

    private Scriptable wrapElement(Context context, DomElement element) {
        Scriptable existing = wrappers.get(element);
        if (existing != null) {
            return existing;
        }
        NativeObject object = nativeObject();
        wrappers.put(element, object);
        reverseWrappers.put(object, element);
        ScriptableObject.putProperty(object, "tagName", element.tag().toUpperCase(Locale.ROOT));

        putFunction(object, "addEventListener", args -> {
            if (args.length >= 2 && args[1] instanceof Function function) {
                element.addEventHandler(stringArg(args, 0), function);
            }
            return Undefined.instance;
        });
        putFunction(object, "getAttribute", args -> element.attribute(stringArg(args, 0)));
        putFunction(object, "setAttribute", args -> {
            element.setAttribute(stringArg(args, 0), stringArg(args, 1));
            document.mutated();
            return Undefined.instance;
        });
        putFunction(object, "getText", args -> element.textContent(document.translations()));
        putFunction(object, "setText", args -> {
            element.setRawText(stringArg(args, 0));
            document.mutated();
            return Undefined.instance;
        });
        putFunction(object, "getValue", args -> element.value());
        putFunction(object, "setValue", args -> {
            element.setValue(stringArg(args, 0));
            document.mutated();
            return Undefined.instance;
        });
        putFunction(object, "setHidden", args -> {
            element.setHidden(booleanArg(args, 0));
            document.mutated();
            return Undefined.instance;
        });
        putFunction(object, "isHidden", args -> element.hidden());
        putFunction(object, "addClass", args -> {
            element.addClass(stringArg(args, 0));
            document.mutated();
            return Undefined.instance;
        });
        putFunction(object, "removeClass", args -> {
            element.removeClass(stringArg(args, 0));
            document.mutated();
            return Undefined.instance;
        });
        putFunction(object, "toggleClass", args -> {
            boolean added = element.toggleClass(stringArg(args, 0));
            document.mutated();
            return added;
        });
        putFunction(object, "appendChild", args -> {
            if (args.length == 0 || !(args[0] instanceof Scriptable scriptable)) {
                return false;
            }
            DomElement child = reverseWrappers.get(scriptable);
            if (child == null) {
                return false;
            }
            element.appendChild(child);
            document.mutated();
            return true;
        });
        putFunction(object, "querySelectorAll", args -> {
            String selector = stringArg(args, 0);
            Object[] matches = descendants(element).stream()
                    .filter(candidate -> com.goosethings.tools.client.web.style.CssStylesheet.matches(candidate, selector))
                    .map(candidate -> wrapElement(Context.getCurrentContext(), candidate))
                    .toArray();
            return Context.getCurrentContext().newArray(scope, matches);
        });
        return object;
    }

    private static List<DomElement> descendants(DomElement root) {
        java.util.ArrayList<DomElement> result = new java.util.ArrayList<>();
        for (DomElement child : root.children()) {
            result.add(child);
            result.addAll(descendants(child));
        }
        return result;
    }

    private NativeObject nativeObject() {
        NativeObject object = new NativeObject();
        if (scope != null) {
            object.setParentScope(scope);
            object.setPrototype(ScriptableObject.getObjectPrototype(scope));
        }
        return object;
    }

    private void putFunction(Scriptable object, String name, Invoker invoker) {
        BaseFunction function = new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
            @Override
            public Object call(Context context, Scriptable callScope, Scriptable thisObject, Object[] args) {
                return invoker.invoke(args);
            }
        };
        ScriptableObject.putProperty(object, name, function);
    }

    private <T> T run(Action<T> action) {
        return run(MAX_EXECUTION_NANOS, action);
    }

    private <T> T run(long executionNanos, Action<T> action) {
        if (closed) {
            throw new IllegalStateException("JavaScript sandbox is closed");
        }
        Context context = contextFactory.enterContext();
        contextFactory.beginDeadline(executionNanos);
        try {
            return action.run(context);
        } finally {
            contextFactory.endDeadline();
            Context.exit();
        }
    }

    private static String stringArg(Object[] args, int index) {
        if (index >= args.length || args[index] == null || args[index] == Undefined.instance) {
            return "";
        }
        return Context.toString(args[index]);
    }

    private static boolean booleanArg(Object[] args, int index) {
        return index < args.length && Context.toBoolean(args[index]);
    }

    private static String joinArgs(Object[] args) {
        StringBuilder result = new StringBuilder();
        for (Object arg : args) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Context.toString(arg));
        }
        return result.toString();
    }

    @Override
    public void close() {
        closed = true;
        wrappers.clear();
        reverseWrappers.clear();
        scope = null;
    }

    @FunctionalInterface
    private interface Invoker {
        Object invoke(Object[] args);
    }

    @FunctionalInterface
    private interface Action<T> {
        T run(Context context);
    }

    private static final class SafeContextFactory extends ContextFactory {
        private final ThreadLocal<Long> deadline = new ThreadLocal<>();

        @Override
        protected Context makeContext() {
            Context context = super.makeContext();
            context.setOptimizationLevel(-1);
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setInstructionObserverThreshold(INSTRUCTION_THRESHOLD);
            context.setClassShutter(className -> false);
            return context;
        }

        @Override
        protected void observeInstructionCount(Context context, int instructionCount) {
            Long expires = deadline.get();
            if (expires != null && System.nanoTime() > expires) {
                throw new EvaluatorException("Script execution time limit exceeded");
            }
        }

        private void beginDeadline(long executionNanos) {
            deadline.set(System.nanoTime() + executionNanos);
        }

        private void endDeadline() {
            deadline.remove();
        }
    }
}
