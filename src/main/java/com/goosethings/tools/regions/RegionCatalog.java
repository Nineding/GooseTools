package com.goosethings.tools.regions;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public record RegionCatalog(int schema,List<RegionDefinition> regions,Set<String> deleted) {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final int MAX_JSON=262144;
    public RegionCatalog {
        if(schema!=1)throw new IllegalArgumentException("schema");
        regions=List.copyOf(regions);deleted=Set.copyOf(deleted);
        if(regions.size()>256||deleted.size()>256||regions.stream().map(RegionDefinition::id).distinct().count()!=regions.size())throw new IllegalArgumentException("catalog_size");
        for(String id:deleted)if(!id.matches("[a-z0-9_./-]{1,96}"))throw new IllegalArgumentException("id");
    }
    public RegionDefinition find(String id){return regions.stream().filter(r->r.id().equals(id)).findFirst().orElse(null);}
    public String json(){String result=JSON.toJson(this);if(result.length()>MAX_JSON)throw new IllegalArgumentException("catalog_size");return result;}
    public static RegionCatalog parse(String text){if(text.length()>MAX_JSON)throw new IllegalArgumentException("catalog_size");return Objects.requireNonNull(JSON.fromJson(text,RegionCatalog.class));}
}
