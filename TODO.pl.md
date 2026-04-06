# TODO List

1. Results recording system (open-source Supabase or Firebase/Firestore).
2. Add object animation.


- - -
Ad.2. Add object animation.

- <https://www.youtube.com/watch?v=Yl0kw1sTymM>
- [Dribbble – 2D Shape Animation Examples](<https://dribbble.com/search/2d-shape-animation?utm_source=chatgpt.com>)

> W kontekście gier — procedurale animacje np. obracanie kół, kołysanie samochodu czy prosty „bounce” obiektu to wszystko jest animacja w czasie rzeczywistym bez zestawu klatek — wystarczy zmiana parametrów w kodzie (rotacja, pozycja, skala). To jest właśnie idea proceduralnej animacji, czyli sterowania ruchem obiektów w czasie rzeczywistym za pomocą logiki i matematyki, a nie sekwencji bitmapowych klatek


> Mona to zrobić bez używania gotowych sprite’ów. W LibGDX są trzy sensowne podejścia — od najprostszego do bardziej „growego”.

---

# 1️⃣ Generowanie grafiki programowo (ShapeRenderer / Pixmap)

Jeśli obiekty są proste (np. samochód w autoslalomie), możesz:

* narysować kształt w `Pixmap`
* zapisać jako `Texture`
* wygenerować kilka klatek modyfikując parametry

Przykład generowania jednej klatki:

```kotlin
val pixmap = Pixmap(64, 32, Pixmap.Format.RGBA8888)
pixmap.setColor(Color.RED)
pixmap.fillRectangle(0, 8, 64, 16)

pixmap.setColor(Color.BLACK)
pixmap.fillCircle(12, 4, 6)
pixmap.fillCircle(52, 4, 6)

val texture = Texture(pixmap)
pixmap.dispose()
```

Możesz wygenerować kilka wersji (np. przechył auta w lewo/prawo) i wrzucić je do:

```kotlin
val animation = Animation<TextureRegion>(
    0.1f,
    frame1,
    frame2,
    frame3
)
```

Zalety:

* brak zewnętrznych plików
* pełna kontrola
* mały rozmiar projektu

Wada:

* ręczne rysowanie pikselowe

---

# 2️⃣ Jedna grafika + transformacje (najprostsza animacja)

Nie musisz mieć wielu klatek.

Możesz:

* obracać
* skalować
* zmieniać kolor
* przesuwać punkt obrotu

W LibGDX:

```kotlin
batch.draw(
    texture,
    x, y,
    originX, originY,
    width, height,
    scaleX, scaleY,
    rotation,
    0, 0,
    texture.width, texture.height,
    false, false
)
```

Często wystarczy:

* rotacja przy skręcie
* lekkie „bounce” przy ruchu
* zmiana koloru przy kolizji

To daje animację bez wielu sprite’ów.

---

# 3️⃣ Proceduralna animacja (najbardziej eleganckie)

Zamiast klatek — animujesz parametry:

Przykład:

* koła obracają się zależnie od prędkości
* nadwozie przechyla się zależnie od skrętu
* światła pulsują sinusoidą

```kotlin
val tilt = steering * 5f
val bounce = sin(time * 10f) * 2f
```

To wygląda lepiej niż klatkowa animacja przy małych obiektach.

---

# 4️⃣ Jeśli jednak sprite — ale generowany

Możesz:

* wygenerować atlas sprite’ów przy buildzie
* albo narysować prosty obiekt w SVG i eksportować serię klatek

Nie musisz używać cudzej grafiki — możesz zrobić minimalistyczny styl (np. geometryczny).

# 1️⃣ Grafika i animacja — najmniej pracy, najlepszy efekt

Najbardziej opłacalne będzie:

**✔ Jeden prosty sprite (lub prosty kształt)**
**✔ Animacja przez transformacje (rotacja, tilt, scale, bounce)**

To daje:

* minimalny nakład pracy
* brak ręcznego tworzenia wielu klatek
* bardzo dobry efekt wizualny przy małych obiektach

W praktyce:

* skręt → lekki przechył (rotation)
* prędkość → delikatny bounce (sinus)
* kolizja → chwilowe scale + zmiana koloru
* koła → osobna rotacja zależna od prędkości

W 2D często **proceduralna animacja wygląda lepiej niż klatkowa**, jeśli styl jest minimalistyczny.

---