## ClashR for Android

A Graphical user interface of [clash](https://github.com/Dreamacro/clash) for Android

Download from [Releases](https://github.com/Kr328/ClashForAndroid/releases)

### Feature

Fully feature of [clash](https://github.com/Dreamacro/clash)

### ShadowsocksR Protocol

Do not use ShadowsocksR Protocol if not necessary

*Not support AEAD method yet.*

**Encrypting algorithm**
- aes-128-cfb
- aes-192-cfb
- aes-256-cfb
- aes-128-ctr
- aes-192-ctr
- aes-256-ctr
- aes-128-ofb
- aes-192-ofb
- aes-256-ofb
- des-cfb
- bf-cfb
- cast5-cfb
- rc4-md5
- chacha20
- chacha20-ietf
- salsa20
- camellia-128-cfb
- camellia-192-cfb
- camellia-256-cfb
- idea-cfb
- rc2-cfb
- seed-cfb
- none

**Obfs**
- plain
- http_simple
- http_post
- random_head
- tls1.2_ticket_auth

**Protocol**
- origin
- verify_sha1 aka. one time auth(OTA)
- auth_sha1_v4
- auth_aes128_md5
- auth_aes128_sha1
- auth_chain_a
- auth_chain_b

### Requirement

* Android 7.0+
* `armeabi-v7a` , `arm64-v8a`, `x86` or `x86_64` Architecture

### License

See also [LICENSE](./LICENSE) and [NOTICE](./NOTICE)



###  Privacy Policy

See also [PRIVACY_POLICY.md](./PRIVACY_POLICY.md)



### Build

1. Update submodules

   ```bash
   git submodule update --init --recursive
   ```

2. Install `JDK 1.8`, `Android SDK` ,`Android NDK` and `Golang`

3. Create `local.properties` in project root with 

   ```properties
   sdk.dir=/path/to/android-sdk
   ndk.dir=/path/to/android-ndk
   appcenter.key=<AppCenter Key>    # Optional, from "appcenter.ms"
   ```

4. Create `keystore.properties` in project root with

   ```properties
   storeFile=/path/to/keystore/file
   storePassword=<key store password>
   keyAlias=<key alias>
   keyPassword=<key password>
   ``` 

5. Build

   ```bash
   ./gradlew app:assembleRelease
   ```

6. Pick `app-release-<arch>.apk` in `app/build/outputs/apks`
