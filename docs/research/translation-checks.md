# Translation checks

These patterns flag lines for review and include false positives. The confirmed corrections and the new Settings translations still need native review.

## Lowercase letter after a full stop

### fr (33 matching lines)

```text
values-fr/strings.xml:152:    <string name="how_it_works_step_focus_highlight">Turbo s’arrête quand son timer s’arrête. vous pouvez l’arrêter plus tôt à tout moment.</string>
values-fr/strings.xml:214:    <string name="address_changed_message">Les nouvelles vérifications seront comparées aux versions de la vieille page. gardez votre historique ou commencez-vous frais?</string>
values-fr/strings.xml:224:    <string name="schedule_save_failed">Vous ne pouvez pas appliquer le calendrier. veuillez essayer à nouveau.</string>
values-fr/strings.xml:234:    <string name="capture_mode_baseline">Les méthodes de changement commencent une nouvelle base. l’histoire existante est maintenue.</string>
values-fr/strings.xml:252:    <string name="check_failure_timeout_detail">La page n’a pas répondu à temps. essayez de nouveau plus tard.</string>
values-fr/strings.xml:254:    <string name="check_failure_login_detail">Cette page requiert l’accès au moniteur n’a pas. vérifier dans votre navigateur.</string>
values-fr/strings.xml:264:    <string name="check_failure_storage_full_detail">Libérer le stockage et essayer à nouveau. votre historique sauvée est conservée.</string>
values-fr/strings.xml:266:    <string name="check_failure_save_failed_detail">Essayez de nouveau. votre histoire sauvée est conservée. si cela continue à se produire, envoyez un rapport.</string>
values-fr/strings.xml:270:    <string name="check_failure_content_detail">Cette réponse ne pouvait pas être capturée. essayez une autre méthode de capture.</string>
values-fr/strings.xml:289:    <string name="comparison_read_failed">Je ne peux pas mettre à jour l’état de lecture. essayez à nouveau.</string>
```

### ru (35 matching lines)

```text
values-ru/strings.xml:169:    <string name="logs_hr_ago">%1$d г. назад</string>
values-ru/strings.xml:239:    <string name="capture_mode_baseline">Переходные методы начинают новую базовую линию. существующая история сохраняется.</string>
values-ru/strings.xml:257:    <string name="check_failure_timeout_detail">Страница не ответила вовремя. попробуйте снова позже.</string>
values-ru/strings.xml:259:    <string name="check_failure_login_detail">Эта страница требует доступа к монитору не имеет. проверьте его в вашем браузере.</string>
values-ru/strings.xml:261:    <string name="check_failure_access_denied_detail">Сайт отказался от этого проверки. проверите доступ в вашем браузере.</string>
values-ru/strings.xml:271:    <string name="check_failure_save_failed_detail">Попробуйте снова. ваша сохранившаяся история сохраняется. если она продолжается, отправьте отчет.</string>
values-ru/strings.xml:275:    <string name="check_failure_content_detail">Эта реакция не может быть зафиксирована. попробуйте другой метод зафиксирования.</string>
values-ru/strings.xml:280:    <string name="legacy_threshold_description">Этот старший фильтр игнорирует изменения ниже байта. он сравнивает хранимые данные, не видимые пиксели. удалить его, чтобы использовать выбранное правило без этого дополнительного фильтра.</string>
values-ru/strings.xml:294:    <string name="comparison_read_failed">Статус чтения не может быть обновлен. попробуйте еще раз.</string>
values-ru/strings.xml:305:    <string name="browser_text_fallback">Страница рендера не ответила. вот текст из вашей сохранившейся версии.</string>
```

### id (56 matching lines)

```text
values-id/strings.xml:138:    <string name="how_it_works_step_add_highlight">Pemeriksaan yang sukses pertama menyimpan titik permulaan. peringatan dimulai dengan perubahan berikutnya.</string>
values-id/strings.xml:168:    <string name="monitor_editor_error_save">Saya tidak bisa menyimpan halaman ini. coba lagi.</string>
values-id/strings.xml:214:    <string name="schedule_save_failed">Anda tidak bisa menggunakan jadwal. silakan coba lagi.</string>
values-id/strings.xml:218:    <string name="storage_history_policy">Versi yang lebih tua dihapus lebih dari 250 MB. dua versi terbaru dari setiap halaman selalu disimpan, bahkan jika mereka melebihi anggaran ini.</string>
values-id/strings.xml:224:    <string name="capture_mode_baseline">Metode switching memulai garis dasar baru. sejarah yang ada disimpan.</string>
values-id/strings.xml:231:    <string name="capture_preview_empty">Tidak ditemukan teks yang dapat dibaca. halaman ini mungkin membutuhkan pemantauan screenshot.</string>
values-id/strings.xml:242:    <string name="check_failure_timeout_detail">Halaman ini tidak menjawab tepat waktu. coba lagi nanti.</string>
values-id/strings.xml:244:    <string name="check_failure_login_detail">Halaman ini memerlukan akses ke monitor yang tidak ada. periksa di browser Anda.</string>
values-id/strings.xml:246:    <string name="check_failure_access_denied_detail">Situs web menolak pemeriksaan ini. memeriksa akses di browser Anda.</string>
values-id/strings.xml:252:    <string name="check_failure_server_detail">Situs ini mengembalikan kesalahan. coba lagi nanti.</string>
```

### de (3 matching lines)

```text
values-de/strings.xml:170:    <string name="monitor_editor_error_invalid_url">Gib eine vollständige Seitenadresse ein, z. B. example.com.</string>
values-de/strings.xml:310:    <string name="region_picker_ignore_hint">Wähle Inhalte zum Ignorieren, z. B. einen Countdown. Erneutes Auswählen stellt sie wieder her.</string>
values-de/strings.xml:449:    <string name="turbo_row_quiet">Keine Änderung · vor %1$d Sek. geprüft</string>
```

### es (1 matching line)

```text
values-es/strings.xml:106:    <string name="comparison_capture_changed_supporting">Estas dos versiones se capturaron con métodos distintos (texto vs. captura de pantalla), así que no se pueden comparar directamente.</string>
```

### it (1 matching line)

```text
values-it/strings.xml:106:    <string name="comparison_capture_changed_supporting">Queste due versioni sono state acquisite con metodi diversi (testo vs. schermata), quindi non possono essere confrontate direttamente.</string>
```

### pt (1 matching line)

```text
values-pt/strings.xml:106:    <string name="comparison_capture_changed_supporting">Estas duas versões foram capturadas com métodos diferentes (texto vs. captura de tela), então não podem ser comparadas diretamente.</string>
```

## Full stop adjacent to text

### fr (8 matching lines)

```text
values-fr/strings.xml:174:    <string name="website_url_placeholder">https://example.com</string>
values-fr/strings.xml:175:    <string name="monitor_editor_error_invalid_url">Saisissez une adresse complète, comme example.com.</string>
values-fr/strings.xml:290:    <string name="comparison_read_changed">La dernière mise à jour a changé.Ouvrez-la pour le marquer comme lu.</string>
values-fr/strings.xml:412:    <string name="turbo_sheet_no_pages">Pas encore de pages actives.Récupérer une page avant de commencer Turbo.</string>
values-fr/strings.xml:593:    <string name="advice_temporary">Le site est occupé ou descendu.C’est généralement temporaire; nous continuerons à essayer.</string>
values-fr/strings.xml:600:    <string name="advice_generic">Quelque chose s’est passé mal au cours de la dernière vérification.Votre histoire est sûre.</string>
values-fr/strings.xml:757:    <string name="visual_no_areas">Rien de visible n’a changé.La page peut être légèrement déplacée.</string>
values-fr/strings.xml:792:    <string name="ignore_small_off_body">Pas de filtre de taille.Les différences de couleur mineures sont toujours ignorées.</string>
```

### ru (9 matching lines)

```text
values-ru/strings.xml:150:    <string name="how_it_works_step_add_highlight">Первая успешная проверка сохраняет исходную точку.Аларты начинаются с следующей перемены.</string>
values-ru/strings.xml:179:    <string name="website_url_placeholder">https://example.com</string>
values-ru/strings.xml:180:    <string name="monitor_editor_error_invalid_url">Введите полный адрес страницы, например example.com.</string>
values-ru/strings.xml:246:    <string name="capture_preview_empty">Читаемый текст не был найден.На этой странице может понадобиться мониторинг экрана.</string>
values-ru/strings.xml:273:    <string name="check_failure_content_too_large_detail">Эта страница превышает границу размеров записи.Выберите меньшую страницу или другой метод записи.</string>
values-ru/strings.xml:401:    <string name="preview_single_visual_region">Мониторинг экрана поддерживает одну выбранную область.Выберите один элемент или переключите текст для просмотра нескольких областей.</string>
values-ru/strings.xml:623:    <string name="advice_generic">Что-то случилось не так во время последней проверки.Ваша история безопасна.</string>
values-ru/strings.xml:812:    <string name="browser_session_note">Текст и экранные изображения используют профиль браузера в приложении.Подпис или переключение учетных записей на сайте влияет на часы для этого сайта.Сеансы могут истекать.Кодные проверки не используют браузерные cookie-файлы.</string>
values-ru/strings.xml:824:    <string name="ignore_small_off_body">Никакого фильтра размера.Меньшие цветовые различия все еще игнорируются.</string>
```

### id (7 matching lines)

```text
values-id/strings.xml:164:    <string name="website_url_placeholder">https://example.com</string>
values-id/strings.xml:165:    <string name="monitor_editor_error_invalid_url">Masukkan alamat halaman yang lengkap, misalnya example.com.</string>
values-id/strings.xml:204:    <string name="address_changed_message">Pemeriksaan baru akan dibandingkan dengan versi halaman lama.Tahan sejarah Anda atau mulai segar?</string>
values-id/strings.xml:238:    <string name="comparison_no_readable_changes_detail">Teks yang dapat dibaca tidak berubah.Cek Rendered atau Sumber untuk perbedaan lain.</string>
values-id/strings.xml:260:    <string name="check_failure_content_detail">Jawaban ini tidak dapat ditangkap.Coba metode penangkapan lain.</string>
values-id/strings.xml:280:    <string name="comparison_read_changed">Update terbaru telah berubah.Buka untuk menandai sebagai dibaca.</string>
values-id/strings.xml:370:    <string name="external_open_failed">Tidak bisa membuka aplikasi lain.Memasang aplikasi yang kompatibel atau coba lagi.</string>
```

### ar (40 matching lines)

```text
values-ar/strings.xml:164:    <string name="how_it_works_step_focus_highlight">Turbo يتوقف عند انتهاء موعده.يمكنك إيقافه في أي وقت مبكر.</string>
values-ar/strings.xml:189:    <string name="website_url_placeholder">https://example.com</string>
values-ar/strings.xml:190:    <string name="monitor_editor_error_invalid_url">أدخل عنوان صفحة كاملًا، مثل example.com.</string>
values-ar/strings.xml:229:    <string name="address_changed_message">سوف تقارن التحققات الجديدة مع الإصدارات من الصفحة القديمة.حافظ على تاريخك أو تبدأ طازجة؟</string>
values-ar/strings.xml:249:    <string name="capture_mode_baseline">بدأت أساليب التبديل خطًا أساسيًا جديدًا.يتم الحفاظ على التاريخ الحالي.</string>
values-ar/strings.xml:256:    <string name="capture_preview_empty">لم يتم العثور على نص قابل للقراءة.قد تحتاج هذه الصفحة إلى مراقبة صور الشاشة.</string>
values-ar/strings.xml:259:    <string name="requested_comparison_unavailable_description">قد يتم إزالة إحدى أو كلا الإصدارين المحفوظين.يمكنك فتح أحدث إصدار متاح بدلا من ذلك.</string>
values-ar/strings.xml:263:    <string name="comparison_no_readable_changes_detail">النص القابل للقراءة غير قابل للتغيير.تحقق من Rendered أو Source للحصول على اختلافات أخرى.</string>
values-ar/strings.xml:269:    <string name="check_failure_login_detail">هذه الصفحة تتطلب الوصول إلى الشاشة لا يوجد.تحقق من ذلك في المتصفح الخاص بك.</string>
values-ar/strings.xml:271:    <string name="check_failure_access_denied_detail">رفض الموقع هذا التحقق.تحقق من الوصول في المتصفح الخاص بك.</string>
```

## First-person phrasing in French

### fr (4 matching lines)

```text
values-fr/strings.xml:178:    <string name="monitor_editor_error_save">Je ne pouvais pas sauver cette page, essayez à nouveau.</string>
values-fr/strings.xml:265:    <string name="check_failure_save_failed">Je ne pouvais pas faire le check</string>
values-fr/strings.xml:289:    <string name="comparison_read_failed">Je ne peux pas mettre à jour l’état de lecture. essayez à nouveau.</string>
values-fr/strings.xml:699:    <string name="editor_preview_failed_title">Je ne pouvais pas préparer la prévision</string>
```

## First-person phrasing in Russian

### ru (1 matching line)

```text
values-ru/strings.xml:183:    <string name="monitor_editor_error_save">Я не смог бы сохранить эту страницу, попробуй еще раз.</string>
```
