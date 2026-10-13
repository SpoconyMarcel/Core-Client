# Core Client

Launcher Minecrafta: wersje od 1.8.8 do najnowszej, logowanie Microsoft lub offline, silnik Fabric, partner Depresja Client.

## Pobieranie
Zakładka **Releases** na GitHubie: Windows `.exe`, macOS `.dmg`, Linux `.AppImage`.

## Funkcje
- ekran logowania: konto Microsoft albo offline (nick wpisujesz raz, konta są zapamiętane i można je przełączać)
- tokeny Microsoft są szyfrowane systemowym magazynem kluczy
- wszystkie wersje od 1.8.8, wyszukiwarka, silnik Fabric (domyślny) lub Vanilla
- Fabric od 1.14, starsze wersje przez Legacy Fabric (1.8.9, 1.12.2 i inne; instalacja automatyczna razem z Fabric API)
- każda wersja i silnik ma osobny katalog gry w `.coreclient/instances/`, więc mody różnych wersji się nie mieszają
- sprawdzanie Javy przed startem (8 / 17 / 21 zależnie od wersji)
- RAM, pełny ekran, argumenty JVM, ukrywanie launchera podczas gry, logi
- ustawienia modułów HUD i bindów zapisują się do `instances/<wersja>-<silnik>/config/coreclient.json` (czyta je mod Core)

## Uruchomienie z kodu
Node.js 18+ i Java.

    npm install
    npm start

## Wrzucenie na GitHuba i wydanie
1. Utwórz puste repozytorium `core-client` na github.com.
2. W `package.json` zmień `TWOJ_NICK` na swój nick GitHub.
3. W folderze projektu:

       git init
       git add .
       git commit -m "Core Client"
       git branch -M main
       git remote add origin https://github.com/TWOJ_NICK/core-client.git
       git push -u origin main

4. Wydanie (GitHub Actions zbuduje instalatory na 3 systemy):

       git tag v0.2.0
       git push origin v0.2.0

W repo: Settings, Actions, General, Workflow permissions: Read and write.

## Do zrobienia
- Mod Core dla Legacy Fabric (1.8.9 i starsze) oraz dla nowszych wersji niż 1.21.11.
- Własne logo i grafiki.

## Mod Core (folder `mod/`)
Mod Fabric dla Minecrafta 1.21.11 (Java 21): HUD z koordynatami, FPS i CPS, Keystrokes, Armor Status, Full Vision, Zoom, Toggle Sprint oraz menu główne Core z własnym tekstem wersji. Ustawienia i bindy czyta z `config/coreclient.json`, który zapisuje launcher.

Budowanie:
1. Potrzebne: Java 21 i Gradle 9+ (albo skopiuj `gradlew` i folder `gradle/` z `FabricMC/fabric-example-mod`, gałąź 1.21.11).
2. `cd mod && gradle build`, jar pojawi się w `mod/build/libs/`.
3. Wrzuć jar do folderu `mods/` w launcherze. Przy wersji 1.21.11 z silnikiem Fabric launcher sam pobierze Fabric API i skopiuje moda do katalogu gry.

GitHub Actions (`mod.yml`) buduje moda przy każdej zmianie w `mod/` i udostępnia jar jako artefakt.

Mod nie był kompilowany przy pisaniu. Jeśli build zgłosi błąd nazwy klasy lub metody, to różnica w mapowaniach Yarn i wystarczy ją poprawić.

Nie ma jeszcze: Freelook i moda Core dla wersji innych niż 1.21.11.

## Menu modułów w grze i Modrinth
- W grze prawy Shift (zmienisz w Bindy, pozycja „Menu Core") otwiera menu, w którym włączasz i wyłączasz moduły HUD (koordynaty, FPS, CPS, Keystrokes, Armor Status, Zoom, Toggle Sprint, Full Vision). Wybór zapisuje się w `config/coreclient-hud.json` i ma pierwszeństwo przed ustawieniami z launchera. Aby wrócić do ustawień z launchera, usuń ten plik.
- W launcherze: Mody, zakładka Modrinth. Wyszukujesz mody pasujące do wybranej wersji gry (silnik Fabric), klikasz Zainstaluj i launcher pobiera plik razem z wymaganymi zależnościami, sprawdzając sumę SHA-512. Zakładka Zainstalowane pokazuje i usuwa pobrane mody.
