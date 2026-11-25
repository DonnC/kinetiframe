# adb helpers for the UX review panel. Usage, from a POSIX shell:
#   export KF_DEVICE=<adb serial> KF_UX_OUT=notes/ux-review/<date>/<persona>
#   source tools/ux-review/phone.sh
# KF_PACKAGE defaults to the debug build.
: "${KF_DEVICE:?Set KF_DEVICE to the adb serial of the phone to test}"
KF_PACKAGE="${KF_PACKAGE:-com.donnclab.kinetiframe.dev}"
KF_UX_OUT="${KF_UX_OUT:-notes/ux-review/scratch}"
A="adb -s $KF_DEVICE"

# Screenshot to $KF_UX_OUT/<name>.png, printing the path.
shot() { mkdir -p "$KF_UX_OUT"; timeout 20 $A exec-out screencap -p > "$KF_UX_OUT/$1.png"; echo "$KF_UX_OUT/$1.png"; }

# On-screen text and accessibility labels with bounds: label|x1|y1|x2|y2
labels() {
  MSYS_NO_PATHCONV=1 timeout 30 $A shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1
  MSYS_NO_PATHCONV=1 timeout 20 $A shell cat /sdcard/u.xml \
    | grep -o '\(text\|content-desc\)="[^"]\+"[^>]*bounds="[^"]*"' \
    | sed -E 's/(text|content-desc)="([^"]*)".*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\2|\3|\4|\5|\6/'
}

# Tap the centre of the first element labelled exactly with the given text.
tapl() {
  local line; line=$(labels | grep -F "$1|" | head -1)
  [ -z "$line" ] && { echo "missing: $1"; return 1; }
  IFS='|' read -r t x1 y1 x2 y2 <<< "$line"
  timeout 10 $A shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 )); echo "tapped $t"
}

tap() { timeout 10 $A shell input tap "$1" "$2"; }
longpress() { timeout 10 $A shell input swipe "$1" "$2" "$1" "$2" 800; }
swipe() { timeout 10 $A shell input swipe "$1" "$2" "$3" "$4" "${5:-300}"; }
back() { timeout 10 $A shell input keyevent KEYCODE_BACK; }
type_text() { timeout 10 $A shell input text "$(echo "$1" | sed 's/ /%s/g')"; }

# One freehand stroke through the points, sent in a single adb call: draw x1 y1 x2 y2 ...
draw() {
  local cmd="input motionevent DOWN $1 $2" lx=$1 ly=$2; shift 2
  while [ $# -ge 2 ]; do cmd="$cmd; input motionevent MOVE $1 $2"; lx=$1; ly=$2; shift 2; done
  timeout 60 $A shell "$cmd; input motionevent UP $lx $ly" >/dev/null 2>&1
}

# A rough circle of radius r around cx cy.
circle() {
  local cx=$1 cy=$2 r=$3 pts=""
  for a in 0 30 60 90 120 150 180 210 240 270 300 330 360; do
    pts="$pts $(awk -v cx="$cx" -v cy="$cy" -v r="$r" -v a="$a" \
      'BEGIN { printf "%d %d", cx + r * cos(a * 3.14159 / 180), cy + r * sin(a * 3.14159 / 180) }')"
  done
  draw $pts
}

launch() { timeout 10 $A shell monkey -p "$KF_PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; }
idle_ms() { $A shell dumpsys power | grep -o 'lastUserActivityTime=[0-9]* ([0-9]* ms ago)'; }
