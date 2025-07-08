import re
import os
from datetime import datetime

# Folder containing logs
log_dir = "../logs"

# Regex to extract filename
regex_component = re.compile(r'(?P<component>[^.]+)(-[^.]*)?\.log')

# Log line pattern (example):
# 09:31:47,740 [thread-name] LEVEL class.name - message
log_line_regex = re.compile(
    r'(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+'
    r'\[(?P<thread>[^\]]+)\]\s+'
    r'(?P<level>[A-Z]+)\s+'
    r'(?P<class>[^\s]+)\s+-\s+'
    r'(?P<message>.*)'
)


def parse_log_file(file_path, filename):
    component_match = regex_component.match(filename)
    component = component_match.group("component") if component_match else "unknown"

    print(f"\n🔹 Parsing file: {filename} | component: {component}")
    with open(file_path, "r") as f:
        for line in f:
            line = line.strip()
            match = log_line_regex.match(line)
            if match:
                try:
                    ts = datetime.strptime(match.group("timestamp"), "%Y-%m-%d %H:%M:%S,%f")

                    thread = match.group("thread")
                    level = match.group("level")
                    clazz = match.group("class")
                    message = match.group("message")

                    print(f"[{ts}] [component={component}] [thread={thread}] [level={level}] [class={clazz}] {message}")
                except Exception as e:
                    print(f"[parse error] {line} -> {e}")
            else:
                print(f"[unmatched] {line}")

def main():
    if not os.path.exists(log_dir):
        print(f"Log directory '{log_dir}' does not exist.")
        return

    for fname in os.listdir(log_dir):
        if fname.endswith(".log"):
            file_path = os.path.join(log_dir, fname)
            parse_log_file(file_path, fname)

if __name__ == "__main__":
    main()
