"""Append only the new arcade translation keys; preserve all existing entries."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/goosetools/lang"
ui = {
    "close": ("×", "×"), "pause": ("Pause", "暂停"), "paused": ("Paused", "已暂停"),
    "stats": ("Score: %s   Best: %s", "得分：%s   最佳：%s"), "mine_stats": ("Wins: %s  Best: %s", "连胜：%s   最佳用时：%s"),
    "time": ("%s", "%s"), "waiting": ("Waiting for the server…", "等待服务器……"),
    "snake_length": ("Length: %s", "长度：%s"), "serve": ("Ready…", "准备发球……"),
    "endless_practice": ("Endless practice", "无尽练习"), "first11": ("First to 11", "先得 11 分获胜"),
    "won": ("You win!", "你赢了！"), "lost": ("Game over", "游戏结束"),
    "pause_help": ("Press P or Resume to continue.", "按 P 或点击继续，返回游戏。"),
    "result": ("Score: %s   Time: %s", "得分：%s   用时：%s"), "start": ("Start game", "开始游戏"),
    "resume": ("Resume", "继续游戏"), "keep_going": ("Keep going", "继续挑战更大数字"),
    "next_board": ("Next board", "继续下一盘"), "restart": ("New game", "重新开始"), "exit": ("Close game", "退出小游戏"),
    "mode_endless": ("Mode: Endless", "模式：无尽"), "mode_classic": ("Mode: Classic", "模式：经典"),
    "mines0": ("Beginner: 9 × 9 / 10 mines", "初级：9 × 9 / 10 雷"), "mines1": ("Intermediate: 16 × 16 / 40 mines", "中级：16 × 16 / 40 雷"), "mines2": ("Expert: 30 × 16 / 99 mines", "高级：30 × 16 / 99 雷"),
    "speed0": ("Speed: Slow", "速度：慢速"), "speed1": ("Speed: Normal", "速度：正常"), "speed2": ("Speed: Fast", "速度：快速"),
    "ai0": ("AI: Easy", "AI 难度：简单"), "ai1": ("AI: Normal", "AI 难度：普通"), "ai2": ("AI: Hard", "AI 难度：困难"),
    "pace0": ("Pace: Easy", "节奏：简单"), "pace1": ("Pace: Normal", "节奏：普通"), "pace2": ("Pace: Hard", "节奏：困难"),
    "original_rules": ("Classic rules", "经典规则"),
}
games = {
 "flappy": ("Flappy Bird", "像素小鸟", "Space / click: flap   P: pause", "空格 / 点击：拍翅   P：暂停", "Flap through the pipes. Each passed pair scores a point. A collision ends the run.", "拍翅穿过管道，每通过一组得 1 分。碰到管道或边界即结束，挑战更高分。"),
 "snake": ("Snake", "贪吃蛇", "Arrows / WASD: turn   P: pause", "方向键 / WASD：转向   P：暂停", "Eat to grow. Avoid walls and your own body. Fill the entire board to win.", "吃食物让蛇增长，避开墙壁和自身。填满整个棋盘获胜。"),
 "pong": ("Pong", "Pong 乒乓球", "Mouse / W S / Up Down: paddle   P: pause", "鼠标 / W S / 上下键：挡板   P：暂停", "Return the ball against the AI. Classic: first to 11. Endless practice keeps the match going.", "与 AI 接球对战。经典模式先得 11 分获胜，无尽练习持续计分。"),
 "whack": ("Whac-A-Mole", "打地鼠", "Click a mole   P: pause", "点击地鼠   P：暂停", "Hit the moles before they hide. Misses and empty hits cost a life. Three lives, endless waves.", "在地鼠躲回洞前击中它。漏打或打空会失去生命，三条生命挑战无尽高分。"),
 "minesweeper": ("Minesweeper", "扫雷", "Left: reveal   Right: flag   Middle / double: chord", "左键：揭开   右键：插旗   中键 / 双击：展开", "Reveal all safe cells. Numbers count neighboring mines. First click is safe. Flag mines and chord numbered cells.", "揭开全部安全格获胜。数字表示周围雷数，首点安全。右键标记，数字周围旗数正确时可快捷展开。"),
 "2048": ("2048", "2048", "Arrows / WASD: move   P: pause", "方向键 / WASD：移动   P：暂停", "Slide equal tiles together to reach 2048. Each tile merges once per move. Keep going after winning.", "滑动相同数字合并为 2048，每块每次移动只合并一次。获胜后可继续挑战更大的数字。"),
}
commands = {"unavailable": ("%s needs matching GooseTools", "%s 需要相同版本的 GooseTools"), "busy": ("%s cannot play now", "%s 当前无法开始小游戏"), "opened": ("Opened %s arcade game(s)", "已打开 %s 个小游戏"), "closed": ("Closed %s arcade game(s)", "已关闭 %s 个小游戏")}
for lang, index in [("en_us", 0), ("zh_cn", 1)]:
    additions = {"game.goosetools.ui." + key: pair[index] for key, pair in ui.items()}
    for game, values in games.items():
        for j, key in enumerate(("title", "controls", "help")): additions["game.goosetools." + game + "." + key] = values[j * 2 + index]
    additions.update({"game.goosetools.command." + key: values[index] for key, values in commands.items()})
    path = root / (lang + ".json"); source = path.read_text(encoding="utf-8"); current = json.loads(source)
    pending = {k: v for k, v in additions.items() if k not in current}
    if pending:
        tail = json.dumps(pending, ensure_ascii=False, indent=2)[1:-1].strip("\n")
        path.write_text(source.rstrip()[:-1].rstrip() + ",\n" + tail + "\n}\n", encoding="utf-8")
    print(lang, "appended", len(pending), "keys")
