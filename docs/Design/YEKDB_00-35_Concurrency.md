# YEKDB 00-35 Concurrency

## Kapsam

Sprint 00-35, ayni JVM icindeki YEKDB session'lari icin tablo seviyesinde
concurrency control saglar. Uygulama mevcut transaction ve query katmanlarini
koruyarak ortak bir lock manager uzerinde calisir.

## Garanti edilen davranislar

- `SHARED` ve `EXCLUSIVE` tablo kilitleri desteklenir.
- Ayni owner icin reentrant acquisition ve hold-count takibi yapilir.
- Kilit kimlikleri database path ve tablo adi bazinda normalize edilir.
- Immediate, timeout'lu ve interrupt edilebilir acquisition desteklenir.
- Bekleyen istekler adil kuyrukta tutulur; bekleyen writer'in yeni reader'lar
  tarafindan surekli gecilmesi engellenir.
- Wait-for graph donguleri deadlock olarak algilanir.
- Deadlock kurbani olan aktif transaction tamamen rollback edilir ve tuttugu
  read/write kilitleri serbest birakilir.
- Lock upgrade ve downgrade sirasinda basarisiz conversion eski handle'i korur.
- `READ COMMITTED` SELECT kilitleri statement sonunda; `REPEATABLE READ` ve
  `SERIALIZABLE` SELECT kilitleri transaction sonunda serbest birakilir.
- Lock manager snapshot'i owner hold sayilarini, bekleme kuyrugunu ve wait-for
  bagimliliklarini immutable olarak sunar.

## Temel tipler

- `LockManager`: acquisition, conversion ve snapshot sozlesmesi.
- `InMemoryLockManager`: JVM ici lock tablosu, adil kuyruk ve deadlock algilama.
- `LockHandle`: idempotent ve `AutoCloseable` release handle'i.
- `WaitForGraph`: owner'lar arasindaki bekleme bagimliliklari.
- `LockManagerSnapshot`: lock tablosunun detached ve immutable tanilama modeli.
- `TransactionTableReadLockManager` / `TransactionTableWriteLockManager`:
  transaction katmani adapter'lari.

## Hata modeli

- Immediate uyumsuzluk: `LockConflictException`
- Bekleme suresi asimi: `LockTimeoutException`
- Interrupt: `LockAcquisitionInterruptedException`
- Deadlock: `DeadlockDetectedException`
- Transaction adapter katmani timeout, interrupt ve deadlock durumlarini kendi
  `TransactionLockException` alt tiplerine cevirir.

## Snapshot ornegi

```java
LockManagerSnapshot snapshot = lockManager.snapshot();

int activeResources = snapshot.getActiveResourceCount();
int waitingRequests = snapshot.getWaitingRequestCount();

for (LockManagerSnapshot.ResourceState resource : snapshot.resources()) {
    resource.owners();
    resource.waitingRequests();
}
```

Snapshot koleksiyonlari degistirilemez ve olusturulduktan sonra canli lock
tablosundaki degisikliklerden etkilenmez.

## Kapsam disi

- Process'ler veya makineler arasi distributed locking
- Row/page seviyesinde locking
- MVCC ve snapshot isolation
- Kalici lock tablosu

Bu maddeler sonraki sprintlerde mevcut `LockManager` ve `LockResourceType`
soyutlamalari uzerinden genisletilebilir.

## Dogrulama

Sprint kapanisinda unit, integration ve stress testleri su alanlari kapsar:

- shared/exclusive uyumlulugu ve reentrancy
- timeout, interrupt ve cleanup
- isolation-level lock yasam dongusu
- adil kuyruk ve starvation korumasi
- deadlock algilama ve otomatik transaction rollback
- upgrade/downgrade
- snapshot tutarliligi ve immutability
- paralel reader/writer yukunde mutual exclusion ve kaynak sizintisi
