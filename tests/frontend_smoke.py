from pathlib import Path
import re
import subprocess
import tempfile

html = Path("index.html").read_text(encoding="utf-8")

# The previous broken build accidentally inserted literal backslash-n tokens
# into the JavaScript. They must never reach production again.
assert "\\n" not in html, "literal \\n escape found in index.html"

scripts = re.findall(r"<script(?:\s[^>]*)?>(.*?)</script>", html, flags=re.S)
assert scripts, "no inline JavaScript found"
js = scripts[-1]

with tempfile.NamedTemporaryFile("w", suffix=".js", encoding="utf-8", delete=False) as f:
    f.write(js)
    js_path = f.name

result = subprocess.run(["node", "--check", js_path], capture_output=True, text=True)
assert result.returncode == 0, result.stderr

ids = set(re.findall(r'\bid="([^"]+)"', html))
refs = set(re.findall(r'\$\("([^"]+)"\)', js))
allowed_optional = {
    "aiRiskState", "demoStart", "accountModeState", "demoMode",
    "liveMode", "executionTitle", "demoStop", "brokerTrade",
    "demoState", "monitorState",
}
missing = sorted((refs - ids) - allowed_optional)
assert not missing, f"JavaScript references missing DOM ids: {missing}"

for required in ["activateAiBtn", "aiScanBtn", "markets", "aiMarketTabs", "nav", "mode", "modePicker", "demoStressTest"]:
    assert required in ids, f"required UI element missing: {required}"

for market in ["XAU/USD", "WTI/USD", "USD/ZAR", "BTC/USD", "ETH/USD", "SOL/USD"]:
    assert market in html, f"market missing from frontend: {market}"

print("FRONTEND SMOKE TEST: PASS")
print("JavaScript syntax: PASS")
print("Required controls: PASS")
print("Required markets: PASS")
