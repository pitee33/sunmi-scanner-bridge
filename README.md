# L2 Scan Bridge

Sunmi L2 (Android 7.1 PDA, Zebra scanner) → Windows gép vonalkód-„keyboard wedge”, WiFi-n (vagy USB-n).

```
L2 scan gomb ─▶ com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED broadcast
          ─▶ L2 Scan Bridge app (TCP) ─▶ scan-type.ps1 (Windows) ─▶ SendKeys az aktív ablakba
```

## Miért így?

- A L2-nek **nincs USB-HID „keyboard wedge” módja** — USB-n csak MTP/ADB-ként látszik, a Win nem látja szkennernek.
- **BT-s billentyű-emuláció sem lehetséges**: a BluetoothHIDDevice API csak Android 9-től létezik, a L2 7.1-en fut.
- A scanner viszont gyárilag broadcastolja a beolvasott kódot (`data` extra), erre épül a híd.

## Windows oldal

1. Tűzfal: első indításkor a Windows rákérdez → **Engedélyezés** (privát hálózat).
2. Indítás:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scan-type.ps1
   # opciók: -Port 8989, -NoEnter (ne nyomjon Entert a kód után)
   ```
3. Autostart: Parancsikon a startup mappába (Win+R → `shell:startup`), cél:
   `powershell -ExecutionPolicy Bypass -File "C:\scan-type.ps1"`

A beolvasott kód annak az ablaknak íródik be, ahol a kurzor áll (Excel, böngésző, ERP…), utána Enter — egy hagyományos USB szkenner viselkedése.

## L2 oldal

1. APK telepítés ADB-vel (USB debugging már engedélyezve):
   ```
   adb install app-debug.apk
   ```
2. App megnyitása → gép IP + port (alap 8989) → **Mentés és indítás** → **Teszt küldés**.
3. A „Teszt küldés” a `TEST-123`-at küldi el — ha a gépen megjelenik, a híd él.
4. A háttérszolgáltatás boot után magától indul (START_STICKY + BOOT_COMPLETED).

### Vezetékes mód (WiFi nélkül, USB-n)

PC-n (platform-tools kell) az ADB kapcsolat aktív, majd:
```
adb reverse tcp:8989 tcp:8989
```
és a L2 appban a gép IP-nek a `127.0.0.1`-et add meg. Az app TCP kapcsolatai így USB-n át a gép 8989-es portjára futnak.

## Korlátok

- A SendKeys a Windows aktív billentyűkiosztásával gépel — kiterjesztett (ékezetes) karakterek QR-kódból ritkán problémásak lehetnek.
- Ha a scanner nem küld broadcastot (nem minden firmware-en aktív alapból): a L2 **Scanner Setting** appban a „Data output mode” → Broadcast legyen bekapcsolva.
- APK debug-aláírással készül (saját használatra elég; Play-re nem megy fel).

## Build

Az APK-t GitHub Actions buildeli minden pushra; `v*` tag pushnál release-be kerül az APK:
https://github.com/pitee33/sunmi-scanner-bridge/releases
