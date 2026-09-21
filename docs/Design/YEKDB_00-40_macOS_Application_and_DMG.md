# YEKDB 00-40 — macOS Application & DMG Packaging

## Hedef ve çıktı

Bu sprint, YEKDB v1.0.12'yi Apple Silicon macOS üzerinde harici Java kurulumu gerektirmeden çalıştırılabilen `YEKDB.app` ve dağıtılabilir DMG olarak paketler.

- Uygulama: `yekdb-launcher/dist/YEKDB.app`
- Disk imajı: `yekdb-launcher/dist/YEKDB-1.0.12-arm64.dmg`
- Bundle kimliği: `com.yekdb.app`
- Ana giriş: `com.yekdb.launcher.LauncherMain`
- SQL Engine girişi: `com.yekdb.cli.terminal.TerminalLauncher`
- Runtime: ARM64 JDK 21 (uygulamanın içinde)
- Yerel imza: ad-hoc; Developer ID gerektirmez

## Build

Gereksinimler: Apple Silicon Mac, ARM64 JDK 21, Xcode Command Line Tools ve internet bağlantısı.

```shell
git clone https://github.com/EmreBEYS/YEKDB.git
cd YEKDB
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
./yekdb-launcher/scripts/build-macos.sh 1.0.12
```

Betik çekirdek ve launcher JAR'larını Maven Wrapper ile üretir, `jpackage` ile bağımsız uygulama görüntüsünü oluşturur, bundle'ı ad-hoc imzalar ve doğrulanmış DMG üretir.

## Kurulum ve demo

1. `YEKDB-1.0.12-arm64.dmg` dosyasını çift tıklayın.
2. `YEKDB.app` dosyasını `Applications` kısayoluna sürükleyin.
3. Applications klasöründe YEKDB'yi çift tıklayın.
4. Launcher içindeki **YEKDB'yi Başlat** düğmesi SQL terminalini macOS Terminal'de açar.

Veri ve loglar uygulama bundle'ına yazılmaz:

- Veritabanları: `~/Library/Application Support/YEKDB/data`
- Launcher runtime: `~/Library/Application Support/YEKDB/runtime`
- Loglar: `~/Library/Application Support/YEKDB/logs`

Bu sayede uygulama `/Applications` altında salt okunur kabul edilse bile kullanıcı verileri güvenli ve yazılabilir konumda kalır.

## Gatekeeper — Developer ID olmayan yerel demo

Ad-hoc imza bütünlük sağlar ancak Apple tarafından tanınan geliştirici kimliği sağlamaz. İnternetten veya mesajlaşma uygulamasından indirilen DMG karantina etiketi alırsa:

1. Finder'da YEKDB'ye Control-tıklayın ve **Open / Aç** seçeneğini kullanın.
2. Gerekirse System Settings → Privacy & Security ekranında YEKDB için **Open Anyway / Yine de Aç** seçeneğini onaylayın.
3. Yalnızca dosyanın bu projeden geldiğini doğruladıysanız son çare olarak şu hedefli komutu kullanın:

```shell
xattr -d com.apple.quarantine /Applications/YEKDB.app
open /Applications/YEKDB.app
```

Genel güvenlik ayarlarını kapatmayın ve `spctl --master-disable` kullanmayın.

## Public dağıtım: Developer ID ve notarization

Herkese açık dağıtım için Apple Developer Program üyeliği, `Developer ID Application` sertifikası, hardened runtime ve notarization gerekir. Uygulama Developer ID ile imzalandıktan sonra DMG oluşturulmalı; DMG `notarytool` ile gönderilip başarılı sonuçtan sonra `stapler` ile işaretlenmelidir.

Örnek akış:

```shell
codesign --force --deep --options runtime --timestamp \
  --sign "Developer ID Application: ORGANIZATION (TEAMID)" YEKDB.app
xcrun notarytool submit YEKDB-1.0.12-arm64.dmg \
  --keychain-profile "yekdb-notary" --wait
xcrun stapler staple YEKDB-1.0.12-arm64.dmg
spctl --assess --type open --context context:primary-signature -vv \
  YEKDB-1.0.12-arm64.dmg
```

Developer ID sırrı veya kimliği repoya eklenmemelidir.
