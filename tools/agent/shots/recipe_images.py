"""Turn the captures of tools/agent/shots/recipes.jsonl into the docs' recipe images.

For each capture: the crafting panel's top 71 GUI px (down to the bottom of the grid) and its last 5 for
the bottom edge, at the capture's own scale, with the result's tooltip drawn by
tools/generate_docs_images.py's tooltip() where vanilla would put it for a pointer on the result slot,
from the lines the "*Tooltip" answers gave. The background stays transparent.

    python tools/agent/shots/recipe_images.py run/26.2.x/agent/client
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
import generate_docs_images as docs  # noqa: E402

SHOTS = {
    "clay-bowl-recipe": "bowlTooltip",
    "waterskin-recipe": "skinTooltip",
    "copper-canteen-recipe": "canteenTooltip",
    "distiller-boiler-recipe": "boilerTooltip",
}
PANEL_W, PANEL_H = 176, 166
KEEP_TOP, KEEP_BOTTOM = 71, 5
# The crafting table's result slot, and where vanilla starts a tooltip's text for a pointer on its centre.
RESULT_X, RESULT_Y = 124, 35
TEXT_X, TEXT_Y = RESULT_X + 8 + 12, RESULT_Y + 8 - 12
OUT = ROOT / "docs" / "public" / "screenshots" / "recipes"


def rgb(colour):
    if not colour:
        return (255, 255, 255)
    colour = colour.lstrip("#")
    return tuple(int(colour[i:i + 2], 16) for i in (0, 2, 4))


def answers(queue):
    found = {}
    for line in (queue / "out.jsonl").read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        reply = json.loads(line)
        found[reply.get("id")] = reply
    return found


def main():
    queue = Path(sys.argv[1])
    replies = answers(queue)
    font = docs.Font()
    for name, tooltip_id in SHOTS.items():
        frame = Image.open(queue / "screenshots" / f"{name}.png").convert("RGBA")
        reply = next(r for r in replies.values() if r.get("command") == "client.capture"
                     and Path(r.get("result", {}).get("file", "")).stem == name)
        scale = int(round(reply["result"]["guiScale"]))
        gui_w = math.ceil(frame.width / scale)
        gui_h = math.ceil(frame.height / scale)
        left = (gui_w - PANEL_W) // 2
        top = (gui_h - PANEL_H) // 2

        panel = frame.crop((left * scale, top * scale, (left + PANEL_W) * scale, (top + PANEL_H) * scale))
        lines = [(text, rgb(colour)) for text, colour in
                 zip(replies[tooltip_id]["result"]["lines"], replies[tooltip_id]["result"]["colours"])
                 if not text.startswith("§9")]
        width = max(font.width(text) for text, _ in lines)
        gui_out_w = max(PANEL_W, TEXT_X + width + 5)
        gui_out_h = KEEP_TOP + KEEP_BOTTOM
        out = Image.new("RGBA", (gui_out_w * scale, gui_out_h * scale), (0, 0, 0, 0))
        out.alpha_composite(panel.crop((0, 0, PANEL_W * scale, KEEP_TOP * scale)), (0, 0))
        out.alpha_composite(panel.crop((0, (PANEL_H - KEEP_BOTTOM) * scale, PANEL_W * scale, PANEL_H * scale)),
                            (0, KEEP_TOP * scale))

        layer = Image.new("RGBA", (gui_out_w, gui_out_h), (0, 0, 0, 0))
        docs.tooltip(layer, font, TEXT_X, TEXT_Y, lines)
        out.alpha_composite(layer.resize(out.size, Image.NEAREST))
        out.save(OUT / f"{name}.png", optimize=True)
        print(name, out.size, [text for text, _ in lines])


if __name__ == "__main__":
    main()
