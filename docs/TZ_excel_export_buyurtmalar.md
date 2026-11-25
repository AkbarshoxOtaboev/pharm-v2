# Texnik talab: Buyurtmalar ro'yxatini Excel (.xlsx) ga eksport qilish

> **Cursor uchun ko'rsatma:** Quyidagi talab namunaviy fayl `Xorazm_27_10_2026.xlsx` asosida yozilgan.
> Maqsad: loyihada foydalanuvchi bitta tugma bosib **aynan shu ko'rinishdagi** Excel faylni yuklab ola olsin.
> Avval loyiha stackini (til, framework, ORM, mavjud eksport/fayl yuklash kodi) aniqla va mavjud konvensiyalarga mos yoz.
> Noaniq joy bo'lsa, "Ochiq savollar" bo'limiga qara, taxmin qilgan joylaringni PR/izohda yoz.

---

## 1. Maqsad

Viloyat (region) bo'yicha, tanlangan sana uchun buyurtmalar ro'yxatini bitta `.xlsx` faylga chiqarish. Har bir qator = bitta mijoz buyurtmasi. Fayl operatorlar/kuryerlar bilan ishlash uchun mo'ljallangan: Excel'da ochilganda formulalar ishlaydi, jami summa avtomatik hisoblanadi, takroriy manzil/telefon qizil bilan ajratiladi, A4 landshaft formatda bosib chiqariladi.

## 2. Fayl nomi va umumiy tuzilma

| Parametr | Qiymat |
|---|---|
| Fayl nomi | `{Region}_{DD}_{MM}_{YYYY}.xlsx` → masalan `Xorazm_27_10_2026.xlsx` |
| Varaqlar soni | 1 |
| Varaq nomi | Region nomi (`Xorazm`) |
| MIME | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| Sana manbai | Hisobot sanasi (buyurtma ro'yxati sanasi), `B1` katakda ham chiqadi |

Bitta so'rov = bitta region + bitta sana = bitta fayl. Bir nechta region kerak bo'lsa, har biri alohida fayl (yoki ZIP) bo'ladi. Bu keyingi bosqich, hozir shart emas.

## 3. Varaq maketi (qatorlar)

| Qator | Mazmuni |
|---|---|
| **1** | Sarlavha qatori (balandligi 36). `B1` = sana, `C1` = `"{Region} viloyati"`, `F1:P1` birlashtirilgan = `"Mahsulotlar"`, `Q1:AA1` birlashtirilgan = `"Sovg'alar"` |
| **2** | Ustun sarlavhalari (balandligi ~131). `E2:AA2` matni **90° burilgan** (vertikal) |
| **3 … 2+N** | Ma'lumot qatorlari (N = buyurtmalar soni), 3-qatordan boshlanadi |
| bitta bo'sh qator | Ma'lumotdan keyin 1 ta bo'sh qator (originalda 34-qator) |
| **Jami qatori** | `E` dan `AA` gacha `SUBTOTAL` formulalari, `AB` = `"ИТОГ:"`, `AC:AD` birlashtirilgan = umumiy summa |

Originalda 3–33 qatorlar formulalar bilan oldindan to'ldirilgan (qo'lda qo'shimcha buyurtma kiritish uchun), jami qatori 35-qatorda. **Eksportda** qatorlar soni dinamik bo'ladi: jami qatori ma'lumotdan 2 qator keyin keladi.

Qotirish (freeze panes): `A3` (1–2 qator qotirilgan). *Originalda `A18` turibdi, bu tasodifiy xato, takrorlama.*

## 4. Ustunlar xaritasi

| Ustun | Sarlavha (2-qator, aynan shunday) | Tur | Manba / izoh |
|---|---|---|---|
| A | *(sarlavhasiz)* | formula | Tartib raqami: `=IF(B3<>"", COUNTA($B$3:B3), "")` |
| B | `F.I.O` | matn | Mijoz ismi, o'zgartirmasdan (lotin/kirill aralash, `Контакт #244770`, `Без имени` kabi qiymatlar ham bor) |
| C | `Manzil` | matn | Manzil, erkin matn, o'zgartirmasdan |
| D | `Telofon raqam` | son | 9 xonali son (`337059293`), format `0`. Qo'shimcha raqamlar manzil ichida bo'lishi mumkin |
| E | `Buyurtma soni` | son | Buyurtmalar soni (hozir har qatorda `1`) |
| F | `Д - чой И/Х` | son | Mahsulot miqdori. Narx **550 000** |
| G | `Райхон` | son | Narx **200 000** |
| H | `Д/А эликсир` | son | Narx **700 000** |
| I | `Орегадо БОХ` | son | Narx **1 370 000** |
| J | `Ёстиқ янги` | son | Narx **350 000** |
| K | `Оне шот` | son | Narx **890 000** |
| L | `Ванна учун` | son | Narx **700 000** |
| M | `Қора кўзлар` | son | Narx **275 000** |
| N | `Чуфа ёғи` | son | Narx **275 000** |
| O, P | `Пустой` | bo'sh | Zaxira ustunlar, **yashirin**, narxi yo'q, formulaga kirmaydi |
| Q | `Д - чой И/Х` | son | Sovg'a miqdori |
| R | `Райхон` | son | Sovg'a miqdori |
| S | `Д/А эликсир` | son | Sovg'a miqdori |
| T | `Орегадо БОХ` | son | Sovg'a miqdori |
| U | `Ёстиқ янги` | son | Sovg'a miqdori |
| V | `Оне шот` | son | Sovg'a miqdori |
| W | `Ванна учун` | son | Sovg'a miqdori |
| X | `Қора кўзлар` | son | Sovg'a miqdori (sovg'alarda `Чуфа ёғи` yo'q) |
| Y, Z, AA | `Пустой` | bo'sh | Zaxira ustunlar, **yashirin** |
| AB | `Изоҳ` | matn | Izoh, masalan `29.10 da oladi`, `Call 2` |
| AC | `Мутахассис` | matn | Mas'ul xodim: `"{kod} {Ism Familiya}"` → `303 Ikromxuja Nuriddinxujayev` |
| AD | `Заказ` | formula | Buyurtma summasi (4.2-bo'limga qara) |

**Muhim qoidalar**

- Ustun harflari originalniki bilan **bir xil** bo'lishi shart (`O,P,Y,Z,AA` yashirin bo'lsa ham saqlansin). Boshqa jarayonlar shu harflarga tayanishi mumkin.
- Miqdori yo'q mahsulot katagi **bo'sh** qoldiriladi (`0` yozilmaydi). Formulalar bo'sh katakni 0 deb oladi.
- Sovg'alar (`Q:X`) buyurtma summasiga **kirmaydi**.

### 4.1 Mahsulotlar katalogi

Narxlar kodda qattiq yozilmasin: **konfiguratsiya yoki DB jadvaliga** chiqar (`code`, `column`, `title`, `price`, `sort_order`). Ustunlar tartibi va narxlar shu katalogdan olinadi. Yangi mahsulot qo'shilsa kod o'zgarmasligi kerak (zaxira `Пустой` ustunlar shu uchun).

### 4.2 `AD` (Заказ) formulasi

Original formula (har qator uchun, `{r}` = qator raqami):

```
=F{r}*550000+G{r}*200000+H{r}*700000+I{r}*1370000+J{r}*350000+K{r}*890000+L{r}*700000+M{r}*275000+N{r}*275000
```

- Formulani katalogdan **dinamik yig'**, shunda narxlar o'zgarsa formula ham mos keladi.
- Natija **qiymat sifatida emas, formula sifatida** yozilsin (foydalanuvchi Excel'da miqdorni tahrirlasa summa qayta hisoblansin).
- Format: `#,##0`, qalin, shrift 14.

### 4.3 Jami qatori

- `E{t}` … `AA{t}`: `=SUBTOTAL(9, E3:E{t-1})` (har ustun uchun; `SUBTOTAL` chunki filtr qo'llanganda yashirilgan qatorlar hisobga olinmaydi)
- `AB{t}`: `ИТОГ:` (qalin)
- `AC{t}:AD{t}` birlashtirilgan: `=SUBTOTAL(9, AD3:AD{t-1})`, format `#,##0`, qalin, sarlavha rangi

## 5. Uslub (styling)

Original fayl Google Sheets mavzusi (theme) ranglaridan foydalanadi. Quyida ularning aniq RGB ekvivalentlari. Mavzuga emas, **RGB ga** tayan.

| Element | Rang (RGB) | Qayerda |
|---|---|---|
| Sarlavha foni (ko'k) | `B3CEFB` | `B1:E1`, `F1:P1` (1–2 qator), `B2:N2`, `AB2:AD2`, jami katagi `AC:AD` |
| Sovg'a sarlavhasi (och qizil) | `F7B4AE` | `Q1:AA1`, `Q2:AA2` |
| Ma'lumot: mijoz bloki (kulrang) | `D9D9D9` | `B:E` ma'lumot katakchalari |
| Ma'lumot: mahsulot bloki (oq) | `FFFFFF` | `A`, `F:P`, `AB:AD` |
| Ma'lumot: sovg'a bloki | `FBD9D7` | `Q:AA` ma'lumot katakchalari |

Umumiy:

- **Chegara:** barcha jadval kataklarida ingichka (`thin`) hamma tomondan.
- **Shriftlar:** sarlavha `Calibri 14`; `C1` va `F1` `Arial 16` qalin; ma'lumot `Calibri 12`; `AD` `Calibri 14` qalin. (Hamma shrift `Calibri`/`Arial` bo'lsin, boshqa shrift ishlatma.)
- **Tekislash:** hamma katak vertikal markazda, `wrap text` yoqilgan. `B` va `C` ustunlari chapga, qolganlari markazga.
- **Ustun kengligi:** `A` 5.7, `B` 15.7, `C` 30.7, `D` 20.7, `E:N` 5.7, `Q:X` 5.7, `AB` 15.7, `AC` 20.7, `AD` 20.7. `O,P,Y,Z,AA` yashirin (`hidden = true`).
- **Qator balandligi:** 1-qator 36, 2-qator ~131 (vertikal matn sig'ishi uchun), ma'lumot qatorlari avtomatik (uzun manzillar `wrap` bilan).
- `D` ustuni raqam sifatida, format `0` (ilmiy yozuv `3.37E+08` chiqmasin).

### 5.1 Shartli formatlash (conditional formatting)

- `C` va `D` ustunlarida **takroriy qiymatlar** (`duplicateValues`) → to'q qizil matn `9C0006` + och qizil fon `FFC7CE`. Har ustunga **alohida** qo'lla (`C3:C{last}` va `D3:D{last}`), bitta oraliqqa birlashtirma. Maqsad: bir xil manzil/telefon qayta kiritilganini ko'rsatish.
- *(Past ustuvorlik)* Originalda `T3:T33` uchun `T3=0` bo'lsa matn rangi o'zgaradi. Eksportda bo'sh katak yozilgani uchun bu shart emas.

## 6. Chop etish sozlamalari

- Yo'nalish: **landshaft**, qog'oz **A4**
- Chop etish maydoni: `A1:AD{jami qator}`
- Kenglikka sig'dirish: 1 sahifa eni (`fitToWidth = 1`, `fitToHeight = 0`)
- Maydonlar: chap/o'ng ~0.2", tepa/past ~0.4"

## 7. Ma'lumot modeli (taklif)

Cursor mavjud modelga moslashtirsin. Eksport uchun kerakli maydonlar:

```ts
type OrderRow = {
  customerName: string;        // B
  address: string;             // C
  phone: number | string;      // D (9 xonali)
  orderCount: number;          // E
  items: Record<ProductCode, number>;   // F..N (mahsulot -> miqdor)
  gifts: Record<ProductCode, number>;   // Q..X (sovg'a -> miqdor)
  note?: string;               // AB
  specialist?: string;         // AC  "{kod} {Ism Familiya}"
};

type ExportRequest = { region: string; date: string /* YYYY-MM-DD */ };
```

Tartib: buyurtmalar kelib tushish tartibida (A ustunidagi raqam shu tartibni ko'rsatadi). Aniq saralash qoidasi kerak bo'lsa, `created_at` bo'yicha o'sish.

## 8. API va UI

- **Endpoint:** `GET /api/orders/export?region=Xorazm&date=2026-10-27` → `.xlsx` ni oqim (stream) sifatida qaytaradi.
- **Sarlavhalar:** `Content-Type` (yuqoridagi MIME) va `Content-Disposition: attachment; filename="Xorazm_27_10_2026.xlsx"`.
- **UI:** buyurtmalar ro'yxati sahifasida **"Excel yuklab olish"** tugmasi. Joriy filtr (region, sana) shu tugmaga uzatiladi. Yuklanish paytida loading holati, xato bo'lsa tushunarli xabar.
- **Ruxsat:** faqat tegishli rolga (operator/admin) ruxsat. Fayl shaxsiy ma'lumot (ism, telefon, manzil) saqlaydi, shuning uchun autentifikatsiya shart, loglarda telefon/manzilni chiqarma.
- **Hajm:** yuzlab qator uchun oddiy yozish yetadi. Minglab bo'lsa streaming writer ishlat.

## 9. Texnik tavsiyalar (stackga qarab tanla)

- **Node/TS:** `exceljs` (uslub, birlashtirish, shartli formatlash, formulalar, yashirin ustunlar, chop etish sozlamalari to'liq qo'llab-quvvatlanadi).
- **Python:** `openpyxl` (yoki `xlsxwriter`: yangi fayl yaratish uchun qulay).
- **Mavjud kutubxona** loyihada bo'lsa, shuni ishlat, yangisini qo'shma.
- Formulalarni stringda yoz (`'=SUBTOTAL(9,E3:E40)'`). Kutubxona keshlangan qiymat yozmaydi, Excel ochganda o'zi hisoblaydi. Shuning uchun `SUMIFS/INDEX/MATCH` kabi oddiy funksiyalar bilan cheklan, `XLOOKUP/FILTER/SORT` ishlatma.
- Eksport mantig'ini alohida servis/funksiyaga ajrat (`buildOrdersWorkbook(rows, catalog, meta)`), controller faqat so'rovni qabul qilib faylni qaytarsin, shunda testlash oson bo'ladi.

## 10. Qabul mezonlari (acceptance criteria)

- [ ] Fayl Excel va LibreOffice'da xatosiz ochiladi ("tiklash" so'ramaydi), formula xatolari (`#NAME?`, `#VALUE!`) yo'q.
- [ ] Ustun harflari, sarlavha matnlari (kirill yozuvi bilan), birlashtirilgan kataklar va yashirin ustunlar originalga mos.
- [ ] `A` ustuni avtomatik raqamlanadi, `AD` formula sifatida hisoblanadi, jami qatori `SUBTOTAL` ishlatadi.
- [ ] Narxlar katalogdan olinadi (kodda qattiq yozilmagan).
- [ ] Telefon raqami ilmiy yozuvda (`3.37E+08`) ko'rinmaydi.
- [ ] Takroriy manzil/telefon qizil bilan ajratiladi.
- [ ] Chop etishda A4 landshaft, 1 sahifa eniga sig'adi.
- [ ] Fayl nomi `{Region}_{DD}_{MM}_{YYYY}.xlsx` formatida.
- [ ] Bo'sh natija (0 ta buyurtma) uchun ham fayl chiqadi (faqat sarlavha va nol jami) yoki foydalanuvchiga tushunarli xabar.
- [ ] Unit test: namunaviy ma'lumot bilan fayl yaratilib, quyidagi nazorat qiymatlari tekshiriladi.

### Nazorat qiymatlari (namunaviy fayldan)

Namunada **24 ta buyurtma** (3–26 qatorlar) bor.

| Ko'rsatkich | Faqat 24 buyurtma | Originaldagi jami (tuzatish qatorlari bilan) |
|---|---|---|
| `E` Buyurtma soni | 24 | 18 |
| `F` Д - чой И/Х | 4 | 3 |
| `G` Райхон | 45 | 45 |
| `I` Орегадо БОХ | 1 | 1 |
| `M` Қора кўзлар | 12 | 0 |
| `R` sovg'a Райхон | 17 | 17 |
| **Umumiy summa (`AD` jami)** | **15 870 000** | **12 020 000** |

Birinchi qator uchun tekshiruv: `G3 = 3` → `AD3 = 3 × 200 000 = 600 000`.

## 11. Ochiq savollar (Cursor taxmin qilsin, lekin belgilab qo'ysin)

1. **Tuzatish qatorlari.** Originalda mijozsiz qatorlarda manfiy qiymatlar bor (`E32=-6`, `M32=-12`, `F33=-1`). Bu qaytarish/bekor qilish tuzatishlariga o'xshaydi. Tizimda shunday tushuncha bo'lsa, ularni "tuzatish qatori" sifatida eksport qil, bo'lmasa tashlab ket. Birinchi versiyada **kerak emas**.
2. **Sovg'a qoidasi.** Ma'lumotlarda `Райхон = 3` bo'lsa sovg'a `Райхон = 1` ko'rinadi, lekin `Райхон = 6` bo'lganda sovg'a `3` (qoida bo'yicha `2` bo'lardi), bitta qatorda esa `Орегадо БОХ` olgan mijozga ham `Райхон` sovg'a berilgan. Shuning uchun sovg'alarni **hisoblama**, har buyurtma bilan **alohida saqlangan maydon** sifatida chiqar.
3. **Telefon formati.** Hozir `+998`siz 9 xonali son. Tizimda boshqacha saqlansa, eksportda shu formatga keltir.
4. **Bir nechta region** yoki bir nechta sana uchun ommaviy eksport kerakmi (ZIP)?
5. **Filtr/autoFilter.** `SUBTOTAL` ishlatilgani sababli sarlavha qatoriga `autoFilter` qo'shish foydali bo'lardi (originalda yo'q). Ixtiyoriy.
