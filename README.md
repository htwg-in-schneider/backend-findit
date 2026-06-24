# findIT Backend

Das findIT Backend ist eine Spring-Boot-Anwendung und stellt REST-Endpunkte für verlorene und gefundene Gegenstände, Nutzer, Kategorien und Kontaktanfragen bereit.

## Technologien

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Bean Validation
* H2 für lokale Entwicklung
* MariaDB für Test- und Produktionsdatenbank

## Lokaler Start

```bash
./mvnw spring-boot:run
```

Das Backend läuft lokal standardmäßig unter:

```
http://localhost:8080
```

Die REST-API ist erreichbar unter:

```
http://localhost:8080/api
```

## Lokale Datenbank

Für lokale Entwicklung wird standardmäßig eine H2-In-Memory-Datenbank verwendet.

H2-Konsole:

```
http://localhost:8080/h2-console
```

Standardwerte:

```
JDBC URL: jdbc:h2:mem:finditdb
User: sa
Password: leer lassen
```

## API-Endpunkte

### Einträge

```
GET     /api/items
GET     /api/items/{id}
GET     /api/items/filter
GET     /api/items/search
GET     /api/items/{id}/matches
POST    /api/items
PUT     /api/items/{id}
PUT     /api/items/{id}/return
DELETE  /api/items/{id}
```

### Nutzer

```
GET     /api/users
GET     /api/users/{id}
POST    /api/users
PUT     /api/users/{id}
DELETE  /api/users/{id}
```

### Kategorien

```
GET     /api/categories
POST    /api/categories
PUT     /api/categories/{id}
DELETE  /api/categories/{id}
```

### Kontaktanfragen

```
GET     /api/contact-requests
GET     /api/contact-requests/item/{itemId}
POST    /api/contact-requests
PUT     /api/contact-requests/{id}/status
```

## Environment Variables

Das Backend kann über Environment Variables konfiguriert werden.

### Allgemein

```env
PORT=8080
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Für Deployment muss `APP_CORS_ALLOWED_ORIGINS` auf die echte Frontend-Adresse gesetzt werden:

```env
APP_CORS_ALLOWED_ORIGINS=https://DEINE-FRONTEND-ADRESSE
```

Mehrere erlaubte Frontend-Adressen können mit Komma getrennt werden:

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://findit-frontend.example.com
```

### Datenbank

Für lokale Entwicklung sind keine zusätzlichen Datenbankvariablen nötig.

Für Test oder Produktion mit MariaDB werden diese Variablen gesetzt:

```env
SPRING_DATASOURCE_URL=jdbc:mariadb://SERVER:3306/DATENBANKNAME
SPRING_DATASOURCE_USERNAME=DEIN_USERNAME
SPRING_DATASOURCE_PASSWORD=DEIN_PASSWORT
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_FORMAT_SQL=false
```

Echte Zugangsdaten dürfen nicht im Git-Repository gespeichert werden.

## Validierung

Das Backend validiert Eingaben serverseitig. Dadurch werden ungültige Daten auch dann abgelehnt, wenn ein Nutzer das Frontend umgeht und direkt die REST-API anspricht.

Validiert werden unter anderem:

* Pflichtfelder bei Einträgen
* maximale Feldlängen
* gültige E-Mail-Adressen
* Datum darf nicht in der Zukunft liegen
* Nutzer mit vorhandenen Einträgen dürfen nicht gelöscht werden

Bei Validierungsfehlern liefert das Backend strukturierte JSON-Fehlerantworten zurück.

## CORS

CORS wird zentral über die Datei `CorsConfig.java` konfiguriert. Die erlaubten Frontend-Adressen werden über `APP_CORS_ALLOWED_ORIGINS` gesetzt.

Lokal ist standardmäßig erlaubt:

```
http://localhost:5173
```

Für Deployment muss hier die echte GitHub-Pages-Adresse des Frontends eingetragen werden.

## Deployment

Das Backend soll für die Abgabe auf Render.com deployed werden.

Dafür werden benötigt:

* Backend-Repository auf GitHub
* Render Web Service
* Region Frankfurt
* Docker-Konfiguration oder Render-kompatibler Build
* Produktionsdatenbank
* Environment Variables für Datenbank und CORS

Nach erfolgreichem Deployment zeigt Render eine öffentliche Backend-URL an. Diese URL muss anschließend im Frontend als `VITE_API_BASE_URL` eingetragen werden.

Beispiel:

```env
VITE_API_BASE_URL=https://findit-backend.example.com/api
```
