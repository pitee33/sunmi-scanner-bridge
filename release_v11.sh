#!/usr/bin/env bash
# 1) wait for the main-branch (SPP) build to finish
# 2) on success: tag v1.1, push, wait for the tag build, download the APK
line=$(head -1 "$HOME/.git-credentials")
tok=${line#https://*:}; tok=${tok%@*}
API=https://api.github.com/repos/pitee33/sunmi-scanner-bridge
dist="$HOME/sunmi-scanner/dist"
mkdir -p "$dist"

run_status() {
  curl -s -H "Authorization: token $tok" "$API/actions/runs?per_page=15" > "$dist/runs.json"
  python3 -c "
import json
d = json.load(open('$dist/runs.json'))
r = [x for x in d.get('workflow_runs', []) if x.get('head_branch') == '$1' and x.get('event') == 'push']
if r:
    print(r[0]['status'] + ' ' + str(r[0].get('conclusion')))
else:
    print('waiting -')
"
}

for i in $(seq 1 100); do
  out=$(run_status main)
  echo "[main $i] $out"
  st=${out%% *}
  if [ "$st" = "completed" ]; then
    conc=$(echo "$out" | awk '{print $2}')
    if [ "$conc" != "success" ]; then
      echo "MAIN_BUILD_FAILED"
      exit 1
    fi
    break
  fi
  sleep 12
done

cd "$HOME/sunmi-scanner" || exit 1
git tag -f v1.1 && git push -q -f origin v1.1 || { echo "TAG_PUSH_FAILED"; exit 1; }
echo "TAGGED_V1.1"

for i in $(seq 1 100); do
  out=$(run_status v1.1)
  echo "[v1.1 $i] $out"
  st=${out%% *}
  if [ "$st" = "completed" ]; then
    conc=$(echo "$out" | awk '{print $2}')
    if [ "$conc" = "success" ]; then
      sleep 5
      curl -fsSL -o "$dist/app-debug-v1.1.apk" https://github.com/pitee33/sunmi-scanner-bridge/releases/download/v1.1/app-debug.apk
      ls -la "$dist/app-debug-v1.1.apk"
      sha256sum "$dist/app-debug-v1.1.apk"
      echo BUILD_SUCCESS_V1_1
      exit 0
    else
      echo "BUILD_FAILED conclusion=$conc"
      exit 1
    fi
  fi
  sleep 12
done
echo TIMEOUT
exit 1
