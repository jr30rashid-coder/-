# توثيق الاعتماديات (DEPENDENCIES)

> **ملاحظة مراجعة التراخيص:** الأرقام الواردة أدناه منسوخة حرفياً من `gradle/libs.versions.toml` و`app/build.gradle.kts`. يجب إجراء مراجعة قانونية مستقلة للتراخيص قبل التوزيع التجاري أو العام (`NEEDS_HUMAN_REVIEW` عند غياب ملف ترخيص مرفق).

## الاعتماديات النشطة في `app/build.gradle.kts`
| الاعتمادية / الأداة | الإصدار الفعلي | الغرض | الترخيص المعلن عادةً (للمراجعة) |
|---|---|---|---|
| Android Gradle Plugin (AGP) | `9.1.1` | بناء تطبيق Android | Apache-2.0 (للمراجعة) |
| Kotlin / Compose Plugin | `2.2.10` | لغة Kotlin ومترجم Compose | Apache-2.0 (للمراجعة) |
| Jetpack Compose BOM | `2024.09.00` | واجهة Compose وMaterial 3 والأيقونات | Apache-2.0 (للمراجعة) |
| `androidx.activity:activity-compose` | `1.10.1` | استضافة Compose و`enableEdgeToEdge` | Apache-2.0 (للمراجعة) |
| `androidx.core:core-ktx` | `1.18.0` | امتدادات نواة Android | Apache-2.0 (للمراجعة) |
| `androidx.lifecycle:*` (runtime/viewmodel/compose) | `2.8.7` | إدارة حالة `ViewModel` و`StateFlow` | Apache-2.0 (للمراجعة) |
| `kotlinx-coroutines-android` / `core` | `1.10.2` | البرمجة غير المتزامنة محلياً | Apache-2.0 (للمراجعة) |

## اعتماديات الاختبار
| الاعتمادية | الإصدار الفعلي | الغرض |
|---|---|---|
| `junit:junit` | `4.13.2` | اختبارات الوحدة JVM |
| `org.robolectric:robolectric` | `4.16.1` | اختبار الموارد والـ Manifest محلياً دون محاكي |
| `androidx.test:core` / `ext:junit` | `1.6.1` / `1.3.0` | سياق الاختبار المحلي |
| `kotlinx-coroutines-test` | `1.10.2` | اختبار التدفقات غير المتزامنة |

## الاعتماديات المعطلة / المستبعدة لسياسة عدم الاتصال (v1.0)
تم تعطيل المكونات التالية في `app/build.gradle.kts` لمنع دمج أي أذونات شبكة أو خدمات سحابية:
- `firebase-bom`, `firebase-ai`, `firebase-appcheck-*`, `firebase-firestore`, `firebase-auth`, `google-services`
- `retrofit`, `converter-moshi`, `okhttp`, `logging-interceptor`
- `secrets-gradle-plugin` (لا توجد مفاتيح API)
- `room` و`ksp` و`coil` و`play-services-location` و`camerax` و`accompanist-permissions` (غير مطلوبة في هذه المرحلة)
