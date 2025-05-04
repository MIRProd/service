# Booking Management System

## Descriere
Acest proiect este un sistem de gestionare a rezervărilor pentru apartamente, în care utilizatorii pot rezerva apartamente, adăuga recenzii, și interacționa cu recenziile lăsând "like" și "dislike". De asemenea, se poate verifica disponibilitatea unui apartament pe o perioadă de timp.

### Functionalități:
- Crearea și gestionarea apartamentelor
- Rezervarea unui apartament
- Crearea recenziilor pentru apartamente
- Adăugarea de like-uri și dislike-uri la recenzii
- Verificarea disponibilității unui apartament pe o perioadă de timp

## Entități și relații

### 1. **UserEntity (Utilizator)**

Entitatea `UserEntity` reprezintă un utilizator din sistem. Un utilizator poate crea recenzii și poate face rezervări.

**Atribute**:
- `id`: ID-ul unic al utilizatorului.
- `name`: Numele utilizatorului.
- `email`: Adresa de email a utilizatorului.
- `apartments`: Lista de apartamente pe care utilizatorul le-a închiriat.

### 2. **ApartmentEntity (Apartament)**

Entitatea `ApartmentEntity` reprezintă un apartament disponibil în sistem. Apartamentele sunt gestionate de utilizatori și pot avea recenzii și rezervări.

**Atribute**:
- `id`: ID-ul unic al apartamentului.
- `title`: Titlul apartamentului.
- `location`: Locația apartamentului.
- `pricePerNight`: Prețul pe noapte al apartamentului.
- `userId`: ID-ul utilizatorului care deține apartamentul.
- `bookings`: Lista de rezervări asociate acestui apartament.
- `reviews`: Lista de recenzii ale acestui apartament.
- `numberOfRooms`: Numărul de camere.
- `numberOfBathrooms`: Numărul de băi.
- `amenities`: Lista de facilități (ex. Wi-Fi, TV, balcon etc.).
- `squareMeters`: Suprafața apartamentului.
- `smokingAllowed`: Indică dacă fumatul este permis în apartament.
- `petFriendly`: Indică dacă sunt permise animalele de companie.

### 3. **BookingEntity (Rezervare)**

Entitatea `BookingEntity` reprezintă o rezervare făcută de un utilizator pentru un apartament.

**Atribute**:
- `id`: ID-ul unic al rezervării.
- `apartmentId`: ID-ul apartamentului rezervat.
- `userId`: ID-ul utilizatorului care a făcut rezervarea.
- `startDate`: Data de început a rezervării.
- `endDate`: Data de sfârșit a rezervării.

### 4. **ReviewEntity (Recenzie)**

Entitatea `ReviewEntity` reprezintă o recenzie lăsată de un utilizator pentru un apartament.

**Atribute**:
- `id`: ID-ul unic al recenziei.
- `apartmentId`: ID-ul apartamentului pentru care a fost lăsată recenzia.
- `userId`: ID-ul utilizatorului care a lăsat recenzia.
- `rating`: Rating-ul acordat (de la 1 la 5).
- `comment`: Comentariul recenziei.
- `likes`: Lista utilizatorilor care au dat like acestei recenzii.
- `dislikes`: Lista utilizatorilor care au dat dislike acestei recenzii.

## Funcționalități principale

### 1. **Crearea unui apartament**
   - Utilizatorii pot crea apartamente, definind detalii precum locația, prețul pe noapte, numărul de camere, facilități și altele.

### 2. **Crearea unei rezervări**
   - Utilizatorii pot rezerva un apartament pentru o perioadă specifică, doar dacă sunt autentificați și dacă apartamentul este disponibil în acea perioadă.

### 3. **Crearea unei recenzii**
   - După ce un utilizator a făcut o rezervare, poate lăsa o recenzie pentru apartamentul respectiv, adăugând un rating și un comentariu.

### 4. **Interacțiunea cu recenziile**
   - Utilizatorii pot adăuga "like" și "dislike" la recenzii, iar un utilizator nu poate adăuga ambele acțiuni pentru aceeași recenzie.

### 5. **Verificarea disponibilității unui apartament**
   - Utilizatorii pot verifica dacă un apartament este disponibil într-o perioadă dată, pe baza rezervărilor existente.
