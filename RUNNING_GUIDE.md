# Huong Dan Chay Du An TheAllNewBinance

Tai lieu nay gom 3 nhom lenh:
- Chay/Build toan bo du an
- Chay tung module (core, server, client)
- Chay theo cau hinh (vi du `dev` mode cho client)

## 1. Dieu kien tien quyet

Can co san:
- JDK 21
- Maven 3.9+

Kiem tra nhanh:

```bash
java -version
mvn -version
```

## 2. Chay toan bo du an

### 2.1 Build full (tat ca module)

Chay tai thu muc goc du an (`TheAllNewBinance`):

```bash
mvn -DskipTests package
```

Lenh nay da duoc verify trong workspace va build thanh cong cho:
- `core`
- `server`
- `client`

### 2.2 Chay full he thong (hien trang)

Hien tai `client` da co entrypoint JavaFX (`ClientApp`) va chay duoc.
Module `server` chua co `main` method (file `server/src/main/java/com/auction/server/ServerApp.java` dang rong), nen chua the start server runtime doc lap.

Voi hien trang codebase, "chay full" se la:
1. Build tat ca module (`mvn -DskipTests package`)
2. Chay client de test giao dien/luong UI

## 3. Chay tung phan (tung module)

Tat ca lenh duoi day chay tu thu muc goc du an, tru khi ghi chu khac.

### 3.1 Core module

Build + test module `core` (kem module phu thuoc neu can):

```bash
mvn -pl core -am test
```

Chi build bo qua test:

```bash
mvn -pl core -am -DskipTests package
```

### 3.2 Server module

Build + test module `server`:

```bash
mvn -pl server -am test
```

Chi build bo qua test:

```bash
mvn -pl server -am -DskipTests package
```

Luu y runtime:
- Hien tai chua co entrypoint de `run` server (`main` method chua duoc implement).

### 3.3 Client module (JavaFX)

Khuyen nghi chay truc tiep theo module POM:

```bash
mvn -f client/pom.xml -DskipTests javafx:run
```

Luu y quan trong:
- Khong dung `mvn -pl client -am javafx:run` tu root, vi Maven se khong resolve duoc plugin prefix `javafx` o root aggregator.

## 4. Chay theo cau hinh da set (config/profile)

Trong code client, mode duoc dieu khien bang system properties:
- `app.devMode`
- `app.hotReload`

(duoc doc trong `client/src/main/java/com/auction/client/config/AppConfig.java`)

### 4.1 Chay client o normal mode (mac dinh)

```bash
mvn -f client/pom.xml -DskipTests javafx:run
```

### 4.2 Chay client o dev mode + hot reload

```bash
mvn -f client/pom.xml -DskipTests javafx:run -Dapp.devMode=true -Dapp.hotReload=true
```

### 4.3 Chay client o dev mode nhung tat hot reload

```bash
mvn -f client/pom.xml -DskipTests javafx:run -Dapp.devMode=true -Dapp.hotReload=false
```

## 5. Lenh tien ich thuong dung

Build nhanh tat ca module, bo qua test:

```bash
mvn -DskipTests package
```

Chay test tat ca module:

```bash
mvn test
```

Clean + build lai:

```bash
mvn clean package
```

## 6. Troubleshooting nhanh

Neu gap loi plugin `javafx` khi chay tu root:
- Dung lenh theo module: `mvn -f client/pom.xml ...`

Neu gap loi JavaFX do moi truong khong co GUI:
- Chay tren desktop session co display server (X11/Wayland).

Neu can bo sung "chay full server + client" dung nghia runtime:
- Implement `main` method cho `server` (vi du trong `ServerApp`), sau do bo sung them section start server -> start client theo 2 terminal.
