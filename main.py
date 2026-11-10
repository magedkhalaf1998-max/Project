import argparse
import base64
import os
import sys
from io import BytesIO
from pathlib import Path

import mss
from openai import OpenAI
from PIL import Image


SYSTEM_PROMPT = """You are a helpful learning assistant. Read the question shown in the image and explain how to solve it. If part of the image is unclear, say what you cannot read and ask for clarification instead of guessing. For multiple-choice questions, explain the relevant reasoning before identifying the best answer. Be accurate, concise, and educational."""


def capture_screen() -> Image.Image:
    """Capture the primary monitor. This is only called after an explicit user command."""
    with mss.mss() as screen:
        monitor = screen.monitors[1]
        shot = screen.grab(monitor)
        return Image.frombytes("RGB", shot.size, shot.rgb)


def load_image(args: argparse.Namespace) -> Image.Image:
    if args.image:
        path = Path(args.image).expanduser()
        if not path.is_file():
            raise FileNotFoundError(f"Image file not found: {path}")
        return Image.open(path).convert("RGB")

    image = capture_screen()
    if not args.no_preview:
        print("A screenshot of your primary monitor was captured.")
        print("It may contain private information. Review it before sending it to the AI.")
        image.show()
        answer = input("Send this screenshot to the configured AI provider? [y/N] ").strip().lower()
        if answer not in {"y", "yes"}:
            raise InterruptedError("Cancelled; the screenshot was not sent.")
    else:
        print("Warning: --no-preview skips the screenshot review and confirmation.")
        answer = input("Send the primary-monitor screenshot to the AI provider? [y/N] ").strip().lower()
        if answer not in {"y", "yes"}:
            raise InterruptedError("Cancelled; the screenshot was not sent.")
    return image


def image_data_url(image: Image.Image) -> str:
    # JPEG keeps the request smaller while retaining enough detail for most text.
    buffer = BytesIO()
    image.save(buffer, format="JPEG", quality=90)
    encoded = base64.b64encode(buffer.getvalue()).decode("ascii")
    return f"data:image/jpeg;base64,{encoded}"


def solve(image: Image.Image, model: str) -> str:
    client = OpenAI()
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "Please read and help me understand the question in this image."},
                    {"type": "image_url", "image_url": {"url": image_data_url(image)}},
                ],
            },
        ],
    )
    content = response.choices[0].message.content
    return content or "The AI returned an empty response."


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Ask an AI learning assistant about a question in a screenshot or image."
    )
    parser.add_argument("--image", help="Use an image file instead of capturing the primary monitor.")
    parser.add_argument(
        "--model",
        default=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
        help="Vision-capable OpenAI model (default: OPENAI_MODEL or gpt-4o-mini).",
    )
    parser.add_argument(
        "--no-preview",
        action="store_true",
        help="Do not open a preview; still asks for confirmation before sending a screen capture.",
    )
    args = parser.parse_args()

    if not os.getenv("OPENAI_API_KEY"):
        print("Set OPENAI_API_KEY in your environment before running this program.", file=sys.stderr)
        return 2

    try:
        image = load_image(args)
        answer = solve(image, args.model)
    except InterruptedError as exc:
        print(exc)
        return 1
    except Exception as exc:
        print(f"Could not process the question: {exc}", file=sys.stderr)
        return 1

    print("\nAI response\n-----------")
    print(answer)
    print("\nCheck the explanation and answer; AI can make mistakes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
