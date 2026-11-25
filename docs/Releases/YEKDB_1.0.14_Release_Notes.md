# YEKDB v1.0.14 — Core Stabilization & macOS Updates

YEKDB 1.0.14, mevcut V1 davranışını ve disk biçimlerini koruyan bir kararlılık
sürümüdür. Bu sürüm transaction karar günlüğünde dayanıklılık iyileştirmeleri,
başlangıç kurtarma sırası düzenlemeleri ve macOS launcher güncelleme desteği
içerir.

## Öne çıkanlar

- Core, CLI ve launcher sürümleri `1.0.14` olarak hizalandı.
- V1 veritabanı, tablo ve metadata biçimleri değiştirilmedi.
- Transaction karar günlüğü yazımları dosya kilidiyle korunarak kalıcı
  depolamaya zorlanıyor.
- Sıfır baytlık transaction log dosyaları geçerli V1 başlığıyla onarılıyor.
- Geçersiz veya bilinmeyen log başlıklarının okunması ve genişletilmesi
  engellendi.
- Veritabanı seçimi sırasında kurtarma işlemi katalog yüklenmeden önce
  çalıştırılıyor.

## macOS launcher güncellemesi

- Launcher, mevcut mimariye uygun `YEKDB-x.y.z-arm64.dmg` veya
  `YEKDB-x.y.z-x86_64.dmg` paketini GitHub Releases üzerinden seçebiliyor.
- İndirilen asset'in boyutu ve GitHub tarafından sağlanan SHA-256 özeti
  doğrulanıyor.
- DMG salt okunur bağlanarak bundle kimliği, sürüm, mimari ve codesign
  bütünlüğü kontrol ediliyor.
- Yeni uygulama önce geçici alanda hazırlanıyor ve launcher kapandıktan sonra
  kuruluyor.
- Değiştirme veya yeniden açma başarısız olursa önceki uygulama geri
  yükleniyor.
- Önceki bundle ve kurulum günlüğü tanılama ve kurtarma amacıyla saklanıyor.
- Windows EXE güncelleme yolu korunuyor.

## Uyumluluk

- Java 21 hedefi korunmuştur.
- Veritabanı header sürümü: `1`
- Tablo header sürümü: `1`
- Metadata sürümü: `0.0.6`
- SQL davranışında veya mevcut V1 kullanıcı verilerinde kasıtlı bir biçim
  değişikliği yoktur.

## Doğrulama

- Core: 2.048 test geçti; başarısız veya atlanan test yok.
- Launcher: 18 test; Windows'ta 14 test geçti ve macOS/platform bağımlı 4 test
  atlandı. macOS doğrulamasında 17 test geçti ve 1 platform bağımlı test
  atlandı.
- Transaction log onarımı, geçersiz başlık reddi ve V1 uyumluluğu için
  regresyon kapsamı eklendi.

## Bilinen sınırlamalar

- `TransactionDurabilityLog` henüz fiziksel veya fizyolojik WAL değildir.
- Commit edilmiş sayfa değişiklikleri için redo veya yarım kalmış veri
  değişiklikleri için kalıcı undo desteği yoktur.
- Native Windows kurulumu ve yayımlanmış bir DMG üzerinden macOS uygulama
  değişimi uçtan uca doğrulanmalıdır.
- macOS paketleri ad-hoc imzalıdır; Developer ID imzası veya Apple
  notarization sağlamaz.
- 1.0.12 kullanıcılarının otomatik macOS güncellemesini kullanabilmesi için
  1.0.14'ü ilk kez elle kurması gerekir.

## Dağıtım dosyaları

- `YEKDB-Setup-1.0.14.exe`
- `YEKDB-1.0.14-arm64.dmg`
- Intel macOS desteği sunuluyorsa `YEKDB-1.0.14-x86_64.dmg`
- Windows ve macOS SHA-256 checksum dosyaları

Tam değişiklik karşılaştırması:
https://github.com/EmreBEYS/YEKDB/compare/v1.0.12...v1.0.14
