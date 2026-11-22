#!/usr/bin/env bash
# Polls the GitHub Actions run for tag v1.0, downloads the APK on success.
line=$(head -1 "$HOME/.git-credentials")
tok=${line#https://*:}; tok=${tok%@*}
dist="$HOME/sunmi-scanner/dist"
mkdir -p "$dist"

for i in $(seq 1 120); do
  curl -s -H "Authorization: token $tok" "https://api.github.com/repos/pitee33/sunmi-scanner-bridge/actions/runs?per_page=10" > "$dist/runs.json"
  out=$(python3 -c "
import json
d = json.load(open('$dist/runs.json'))
r = [x for x in d.get('workflow_runs', []) if x.get('head_branch') == 'v1.0']
if r:
    print(r[0]['status'] + ' ' + str(r[0].get('conclusion')))
else:
    print('waiting -')
")
  st=$(echo "$out" | awk '{print $1}')
  conc=$(echo "$out" | awk '{print $2}')
  echo "[$i] $st $conc"

  if [ "$st" = "completed" ]; then
    if [ "$conc" = "success" ]; then
      sleep 5
      curl -fsSL -o "$dist/app-debug.apk" https://github.com/pitee33/sunmi-scanner-bridge/releases/download/v1.0/app-debug.apk
      echo "APK downloaded:"
      ls -la "$dist/app-debug.apk"
      sha256sum "$dist/app-debug.apk"
      echo BUILD_SUCCESS
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
