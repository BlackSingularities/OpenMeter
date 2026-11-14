# Zmiany

## 2.0.1

- Ikona aplikacji bez tła: na ekranie głównym widać sam kolorowy emblemat, spójnie z trybem „Bez tła” w OpenDesktop.

## 2.0.0

Duża przebudowa: nowy wygląd, angielski, nowe mierniki i funkcje.

- Nowy czarno-biały interfejs w stylu OpenDial: płaskie karty, przyciski-pigułki, sprężyste wciśnięcia i dymki z nazwą po przytrzymaniu.
- Motyw ciemny (AMOLED), jasny albo zgodny z systemem.
- Język angielski i polski. Język wybierzesz w ustawieniach (a na Androidzie 13+ także w ustawieniach systemu). Teksty są w standardowych zasobach Androida – nowy język to jeden plik `values-xx/strings.xml`.
- Ekran główny: wyszukiwarka-pigułka, chipy kategorii, przesuwanie palcem między kategoriami, menu ⋮ z nagraniami, dziennikiem, ustawieniami i zgłoszeniami.
- Dziennik odczytów: ikona zakładki w każdym mierniku zapisuje bieżący odczyt z datą i godziną; do wpisów można dodać notatkę, a cały dziennik wysłać jako CSV.
- Nowe mierniki: Generator tonu (sinus, prostokąt, trójkąt, szum, 20 Hz–20 kHz), Linijka na ekranie (cm i cale, z korektą skali) i Bateria (prąd, moc, napięcie, temperatura, stan, szacowana pojemność).
- Jednostki metryczne lub imperialne (°F, stopy, mile) i wybór jednostki ciśnienia (hPa, mmHg, inHg).
- Decybelomierz: alarm hałasu z wibracją po przekroczeniu progu.
- Krokomierz: cel kroków i własna długość kroku.
- Ekran powitalny przy pierwszym uruchomieniu i nowy ekran „O aplikacji” z informacją o prywatności.
- Ustawienia uporządkowane w sekcje: wygląd, jednostki, ogólne, mierniki, dane, aktualizacje; reset kalibracji jednym przyciskiem.

## 1.2.0

- Zgłaszanie błędów i sugestii: nowa pozycja „Zgłoś błąd lub sugestię” – opis trafia prosto do autora przez redzikowski.com, e-mail jest opcjonalny.
- Po nieoczekiwanym zamknięciu aplikacja przy następnym uruchomieniu proponuje wysłanie raportu o błędzie – tylko za zgodą użytkownika.

## 1.1.6

- Bez żadnych wzmianek o GitHubie w aplikacji: nowy adres serwera aktualizacji, komunikaty błędów bez nazw zewnętrznych serwerów.

## 1.1.5

- Komunikat o nowej wersji bez listy zmian, linków i informacji, skąd pobierana jest aktualizacja.

## 1.1.4

- Dopasowane kolorystycznie tła ikon aplikacji i spójne zasoby graficzne.

## 1.1.1

- Nowa strona aplikacji z opisem funkcji.
- Jednolita informacja o autorze (Adam Rędzikowski · redzikowski.com) i przycisk „Postaw kawę”.
- Usunięte odnośniki do repozytorium z interfejsu.

## 1.1.0

Odświeżony wygląd i wygodniejsza obsługa:

- Kolory kategorii: każdy miernik ma własny akcent (dźwięk i światło, położenie, magnetyzm, ruch, otoczenie, urządzenie)
- Ekran główny: wyszukiwarka (także bez polskich znaków), filtry kategorii, ulubione i ostatnio używane
- Przytrzymanie kafelka lub gwiazdka dodaje miernik do ulubionych – trafiają na początek listy
- Układ siatki lub listy do wyboru
- Nowy nagłówek miernika z ikoną i kategorią, poświata w kolorze akcentu, gradientowe wykresy i skale
- Nagrania: podsumowanie, podgląd wykresu każdej kolumny CSV ze statystykami (min/średnia/max), potwierdzenie usuwania, usuwanie wszystkich
- Ustawienia z opisami, resetem kalibracji i czyszczeniem ulubionych
- Płynniejsze przejścia między ekranami

## 1.0.1 — Transparent application icon

- Use the transparent application mark in launcher, splash and existing branding surfaces.
- Preserve alpha in legacy and adaptive Android icons; refresh themed icon resources.

## 1.0.0

Pierwsze wydanie OpenMeter – mierniki i czujniki w jednym miejscu:

- Decybelomierz dB(A)/dB(Z) z widmem, spektrogramem i stroikiem
- Luksomierz z EV i zaleceniami oświetlenia
- Poziomica (2D i na krawędzi) oraz kątomierz
- Wykrywacz metalu z dźwiękiem i kompas z namiarem
- Sejsmograf z oceną intensywności i miernik drgań w podróży (ISO 2631)
- Barometr z wysokością, trendem i historią 24 h
- Klimat, obrotomierz, krokomierz, czujnik zbliżenia, kąt zawiasu (foldy)
- Inspektor wszystkich czujników telefonu
- Nagrywanie pomiarów do CSV (także w Pobrane/OpenMeter)
- Automatyczne aktualizacje z GitHub Releases
