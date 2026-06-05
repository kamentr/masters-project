# Smart Real Estate Advisor for Plovdiv - Plan and Requirements

## Summary

Build a Java Spring MVC web application that recommends residential properties in Plovdiv using an OWL ontology, JADE agents communicating through ACL, ontology manipulation, and a small SQLite database.

Chosen defaults:
- Web stack: Java 17, Spring Boot + Spring MVC, Thymeleaf, Bootstrap, Leaflet map.
- Agents: JADE.
- Ontology: OWL file managed with OWL API.
- Database: SQLite with Spring Data JDBC.
- Data: synthetic CSV with 60–120 realistic Plovdiv property listings.

MVP scope:
- The first complete version must implement search, ranked recommendations, explanations, map markers, property details, CSV import, basic admin edit/unavailable actions, ontology updates, feedback storage, and agent logs.
- Compare, favorites, and manual property creation are secondary features implemented after the core recommendation/import workflow is working.

Main user flow:
1. User selects buyer profile, budget, district preferences, rooms, and lifestyle needs.
2. Spring MVC sends the request into the JADE agent system.
3. Agents communicate through ACL and query/manipulate the ontology.
4. The app returns ranked property recommendations with explanations.
5. User can view properties on a map, leave feedback, and optionally compare results or save favorites after the MVP workflow is complete.

## Functional Requirements

### User Features

- Search for properties in Plovdiv by:
    - buyer profile: Student, Young Professional, Family, Retired Person, Investor
    - budget
    - preferred districts
    - minimum rooms/bedrooms
    - construction type
    - heating type
    - elevator/parking/balcony
    - proximity priorities: school, kindergarten, university, park, pharmacy, hospital, transport stop

- View recommendations as:
    - property cards
    - ranked table
    - map markers using Leaflet/OpenStreetMap
    - recommendation explanation panel

- Compare selected properties by:
    - secondary feature after MVP
    - price
    - area
    - price per square meter
    - district
    - rooms
    - floor/elevator
    - nearby amenities
    - suitability score

- Save user feedback:
    - property rating
    - comment
    - whether recommendation was useful

### Admin Features

- Import synthetic property data from CSV.
- Add a new property manually as a secondary feature after MVP.
- Edit existing property fields.
- Mark property as unavailable.
- Trigger ontology update after import/edit.
- View simple agent communication logs.

### Ontology Features

Create `plovdiv-real-estate.owl` with classes:
- `Property`, `Apartment`, `House`
- `Neighborhood`
- `Amenity`, `School`, `Kindergarten`, `University`, `Hospital`, `Pharmacy`, `Park`, `TransportStop`
- `BuyerProfile`, `StudentProfile`, `FamilyProfile`, `RetiredProfile`, `InvestorProfile`, `YoungProfessionalProfile`
- `ConstructionType`, `HeatingType`
- `RecommendationCategory`

Object properties:
- `locatedInNeighborhood`
- `nearAmenity`
- `hasConstructionType`
- `hasHeatingType`
- `suitableForProfile`
- `recommendedFor`

Data properties:
- `hasPriceEUR`
- `hasAreaSqM`
- `hasPricePerSqM`
- `hasRooms`
- `hasBedrooms`
- `hasFloor`
- `hasTotalFloors`
- `hasYearBuilt`
- `isAvailable`
- `hasElevator`
- `hasParking`
- `hasBalcony`
- `hasLatitude`
- `hasLongitude`
- `hasDistanceToSchool`
- `hasDistanceToKindergarten`
- `hasDistanceToUniversity`
- `hasDistanceToPark`
- `hasDistanceToTransport`
- `hasDistanceToHospital`
- `hasDistanceToPharmacy`

Ontology manipulation:
- Add property individual from imported/admin data.
- Update property values such as price, availability, district, rooms.
- Add/remove suitability relation, for example `Property_001 suitableForProfile FamilyProfile`.
- Save ontology changes back to the OWL file.
- Recalculate deterministic suitability relations after property updates.
- Use an OWL reasoner only for optional validation/class consistency checks; v1 recommendation scoring is rule-based and implemented in application code.

### Agent Requirements

Use JADE agents with ACL communication.

Required agent types:

- `UserRequestAgent`
    - Receives search criteria from Spring service.
    - Sends ACL `REQUEST` to `RecommendationAgent`.

- `RecommendationAgent`
    - Coordinates the recommendation workflow.
    - Sends ACL `REQUEST` messages to `PropertyAgent` and `NeighborhoodAgent`.
    - Combines returned results into ranked recommendations.
    - Sends ACL `INFORM` response back to `UserRequestAgent`.

- `PropertyAgent`
    - Queries ontology for matching properties by price, rooms, district, construction type, heating type.
    - Returns candidate property IDs and base property scores.

- `NeighborhoodAgent`
    - Evaluates amenities and lifestyle suitability.
    - Scores properties for profiles such as Family, Student, Retired Person, Investor.

- `OntologyUpdateAgent`
    - Handles ontology insert/update operations after admin import or edit.
    - Sends success/failure ACL responses.

ACL message format:
- Performative: `REQUEST`, `INFORM`, `FAILURE`
- Content format: JSON string
- Every message includes:
    - `requestId`: UUID generated by `RecommendationService`
    - `type`: command or response type, for example `SEARCH_PROPERTIES`, `SCORE_NEIGHBORHOOD`, `UPDATE_ONTOLOGY`
    - `payload`: request or response data
    - `errors`: empty array on success, validation or processing errors on failure
- Agent requests use a 5 second timeout in v1. If `PropertyAgent` fails, the recommendation returns `FAILURE`. If `NeighborhoodAgent` fails, the app can return property matches with lower confidence and an explanation that lifestyle scoring was unavailable.
- Example request:
```json
{
  "requestId": "89b1d6a9-f6d9-44b6-a0f9-4b27e8a5b8f3",
  "type": "SEARCH_PROPERTIES",
  "payload": {
    "profile": "Family",
    "maxBudgetEUR": 150000,
    "districts": ["Trakia", "Karshiyaka", "Smirnenski"],
    "minBedrooms": 2,
    "requiresElevator": true,
    "priorities": ["School", "Kindergarten", "Park", "TransportStop"]
  },
  "errors": []
}
```

Recommendation response:
```json
{
  "requestId": "89b1d6a9-f6d9-44b6-a0f9-4b27e8a5b8f3",
  "type": "RECOMMENDATION_RESULTS",
  "payload": {
    "results": [
      {
        "propertyId": "P001",
        "score": 92,
        "confidence": "HIGH",
        "explanation": [
          "Within budget",
          "2 bedrooms",
          "420m from school",
          "300m from kindergarten",
          "180m from bus stop",
          "Has elevator"
        ]
      }
    ]
  },
  "errors": []
}
```

## Data and Database Requirements

### Synthetic Property CSV

Create `properties-plovdiv.csv` with 60-120 records.

Fields:
```text
id,title,type,district,priceEUR,areaSqM,rooms,bedrooms,floor,totalFloors,
constructionType,yearBuilt,heatingType,hasElevator,hasParking,hasBalcony,
latitude,longitude,isAvailable,distanceToSchool,distanceToKindergarten,
distanceToUniversity,distanceToPark,distanceToTransport,distanceToHospital,
distanceToPharmacy
```

`pricePerSqM` is calculated during import as `priceEUR / areaSqM` and stored in the ontology as `hasPricePerSqM`.

Districts:
```text
Center, OldTown, Kapana, Kamenitsa, Karshiyaka, Trakia, KuchukParis,
Smirnenski, Komatevo, Proslav, Ostromila, Belomorski
```

### SQLite Tables

Use the database for non-ontology application data:

Persistence choice:
- Use Spring Data JDBC or plain Spring `JdbcTemplate` with SQLite.
- Avoid JPA/Hibernate for v1 to keep the SQLite setup simple and predictable.

- `users`
    - demo users or anonymous user sessions

- `search_history`
    - user criteria, timestamp, selected profile

- `favorites`
    - saved property IDs

- `feedback`
    - property ID, rating, comment, timestamp

- `import_batches`
    - CSV import metadata

- `agent_logs`
    - sender, receiver, performative, message summary, timestamp

Property domain facts live primarily in the ontology after import. The database can store raw import metadata and user/application behavior.

## Web Application Structure

Pages:

- `/`
    - search form and dashboard landing page

- `/search`
    - POST search criteria
    - displays recommendations, explanations, and map

- `/properties/{id}`
    - detailed property page with ontology-derived facts and database feedback

- `/compare`
    - compare selected properties

- `/admin/properties`
    - list/import/edit properties

- `/admin/import`
    - upload CSV and trigger ontology update

- `/admin/agents`
    - view recent ACL communication logs

Services:

- `RecommendationService`
    - bridge between Spring MVC and JADE agent workflow

- `OntologyService`
    - OWL API loading, querying, updating, saving

- `PropertyImportService`
    - parses CSV, validates records, passes update commands to `OntologyUpdateAgent`

- `FeedbackService`
    - stores ratings/comments in SQLite

- `MapService`
    - prepares property and amenity marker data for Leaflet

## Recommendation Rules

Use deterministic scoring for v1.

Base filters:
- Exclude properties where `isAvailable = false`.
- Exclude properties above `maxBudgetEUR`.
- Exclude properties below requested minimum rooms/bedrooms.
- Apply selected construction/heating/district filters before scoring.

Score range:
- Final score is normalized to 0-100.
- Tie-break order: lower price per square meter, then shorter distance to top selected priority, then newer year built.

Shared scoring weights:
- Budget fit: 25 points
- Profile-specific amenity fit: 35 points
- Property feature fit: 25 points
- District preference and connectivity: 15 points

Budget fit:
- 25 points if price is at least 15% below max budget.
- 18 points if price is within max budget.
- 0 points if above budget; property is filtered out.

Family profile:
- budget match
- at least 2 bedrooms
- school within 800m
- kindergarten within 800m
- park within 1000m
- elevator preferred
- parking preferred
- scoring detail: school 10, kindergarten 10, park 7, elevator 4, parking 4

Student profile:
- lower price
- university within 1500m
- transport stop within 500m
- smaller apartment acceptable
- central or university-connected districts preferred
- scoring detail: university 14, transport 10, low price per square meter 6, district fit 5

Retired profile:
- elevator or floor <= 2
- pharmacy within 700m
- hospital within 2000m
- park within 1000m
- transport stop within 500m
- scoring detail: elevator/low floor 10, pharmacy 8, hospital 7, park 5, transport 5

Investor profile:
- good price per square meter
- central/developing district
- university or transport nearby
- small/medium apartment preferred
- scoring detail: price per square meter 15, district growth/centrality 8, university/transport 7, apartment size liquidity 5

Young professional profile:
- transport nearby
- central or well-connected district
- parking preferred
- park or pharmacy nearby optional as a proxy for local services in v1
- scoring detail: transport 12, district connectivity 10, parking 5, park/pharmacy proximity 4, newer construction 4

Each recommendation must include an explanation generated from the matching rules.

## Documentation Requirements

Create approximately 10 pages covering:

1. Project title, goal, and motivation.
2. Assignment requirements mapping.
3. System architecture diagram.
4. Ontology model: classes, properties, individuals.
5. Agent model: agent types, responsibilities, ACL communication.
6. Database design.
7. User interface and main workflows.
8. Ontology manipulation process.
9. Recommendation logic and examples.
10. Testing, limitations, and future improvements.

Include screenshots:
- search page
- recommendation results
- map view
- admin import
- ontology graph or Protégé screenshot
- agent log/ACL communication

## Test Plan

Functional tests:
- Search as Family profile returns properties with family-oriented explanations.
- Search as Student profile prioritizes university/transport proximity.
- Search with too-low budget returns empty result with clear message.
- Property detail page loads ontology facts and database feedback.
- CSV import adds new property individuals to ontology.
- CSV import validates required amenity distance columns.
- Editing price updates ontology value and affects future recommendations.
- Marking property unavailable removes it from recommendations.
- Feedback is saved and displayed.

Agent tests:
- `UserRequestAgent` sends ACL `REQUEST`.
- `RecommendationAgent` sends ACL messages to `PropertyAgent` and `NeighborhoodAgent`.
- Agents return ACL `INFORM` on success.
- Agents return ACL `FAILURE` for invalid request format.
- Agent log is persisted in SQLite.

Ontology tests:
- OWL file loads successfully.
- Imported property becomes an `Apartment` or `House` individual.
- Data properties are correctly attached.
- Suitability object properties are added/updated.
- Ontology is saved after manipulation.

UI tests:
- Search form validates required fields.
- Result cards display score and explanation.
- Leaflet map displays property markers.
- Compare page handles 2-4 selected properties.
- Admin import page reports success/failure.

## Assumptions

- The project targets Plovdiv only.
- The first complete version uses synthetic property data, not live scraping.
- Spring MVC means server-rendered MVC using Thymeleaf, not a separate SPA frontend.
- SQLite is sufficient for the assignment database requirement.
- JADE remains the agent framework because it directly supports ACL communication.
- OpenStreetMap is used only for map visualization in v1; amenity distances are stored in the synthetic CSV rather than dynamically fetched.
- Authentication is not required; admin pages can be accessible directly for coursework/demo purposes.
