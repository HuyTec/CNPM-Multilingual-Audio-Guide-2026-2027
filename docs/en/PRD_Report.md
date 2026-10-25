**PRODUCT REQUIREMENTS DOCUMENT**  
***Multilingual Audio Guide Application***

# **1\. PRODUCT OVERVIEW**

## **1.1. Product Name**

*Multilingual Audio Guide Application*

## **1.2. Product Purpose**

* The Multilingual Audio Guide application helps visitors explore attractions and access verified multilingual guide content through a Tourist Web application or an Android application. Both clients serve the same Tourist actor and implement the same core use cases and business rules.
* The system enables visitors to discover nearby locations, search for attractions, view map locations, access guide content in multiple languages, listen to audio commentary, and use essential guide materials even while offline.  
* The system also includes a Web Admin Dashboard for managing location information, multilingual guide content, audio files, system analytics, and visitor feedback.

## **1.3. Target users**

The system is designed for the following key user groups:

* **Tourists:** Use the Tourist Web or Android application to explore locations, access multilingual guide content, listen to audio commentary, receive location-based notifications, and manage offline content under the platform conditions in Section 2.4. Tourist access does not require an account.
* **Administrators/Content Editors**: Use the web-based administration dashboard to create and maintain location information, tour scripts, and audio files.  
* **System Administrators/Product Owners:** Use the web-based administration dashboard to monitor system usage statistics and manage feedback from tourists.

## **1.4. Product goals**

The main goals of the system are:

* Help tourists easily discover and select tourist attractions.  
* Provide accurate and verified multilingual guide content.  
* Provide audio-based guide experiences for supported locations.  
* Support offline access to essential guide content.  
* Notify tourists when they enter the area of a supported tourist location.  
* Provide administrators with tools to manage location content and audio assets.  
* Provide basic analytics and tourist feedback management to support future service improvements.

**Proposed Success Metrics**: The following metrics support the existing product goals. Bracketed targets require team confirmation; they do not represent measured results. These metrics are evaluated from raw analytics data or acceptance testing; UC-07 itself displays only the four metrics listed in FR-12.

| Goal | Success Metrics | Proposed target/Notes |
| ----- | ----- | ----- |
| Accurate and verified multilingual guide content. | Percentage of supported locations with APPROVED content in at least \[2\] languages. | Target percentage: \[40%\].  |
| Audio-based guide experiences. | Successful audio playback rate. | At least \[95\]%. Measurement conditions and treatment of TTS fallback: \[\].  |
| Easily discover and select attractions. | Time to reach the Location Details Screen. | At most \[10\] s. Measurement start point and test conditions: \[\].  |

# **2\. PRODUCT SCOPE & REQUIREMENTS**

## **2.1. In Scope**

**The system includes the following functional scope:**  
The term **Tourist Application** denotes both Tourist Web and Android. Presentation, navigation, permissions, and platform adapters may differ; content identifiers, approval/publication rules, language fallback, playback requirements, package safety, and feedback status rules remain shared. Web Admin is a separate authorized interface. See Section 2.4 for platform conditions and delivery acceptance.

**Location Discovery and Selection**

*   Discover nearby tourist locations.  
*   Search for locations by keyword.  
*   Browse locations by category.  
*   Select locations from an interactive map.  
*   Open a location through a location-specific URL, including a URL encoded in a QR code, without requiring an Android installation. This is an entry path of UC-01, not a separate use case or QR-management feature.
*   Related Use Case: UC-01 – Explore & Select Location.

**Multilingual Guide Content**

*   Retrieve approved guide content based on location, language, and content mode.  
*   Support FULL and SHORT guide content.  
*   Apply language fallback when the selected language is unavailable.  
*   Related Use Case: UC-02 – Access Multilingual Guide Content.

**Audio Guide Playback**

*   Play pre-rendered audio guide content.  
*   Support Pause, Resume, Seek, and playback speed control.  
*  Use platform-provided Text-to-Speech as a fallback when audio is unavailable.
*   Related Use Case: UC-03 – Play Audio Guide.

**Offline Content Management**

*   Check for approved content updates.  
*   Download and install verified offline packages.  
*   Manage optional downloaded content.  
*   Preserve verified Base Content under the platform baseline rules in Section 2.4.
*   Related Use Case: UC-04 – Manage & Update Offline Content.

**Location-Based Notifications**

*   Detect entry into configured tourist-location geofences.  
*   Display operating-system notifications or In-App Banners.  
*   Open the corresponding Location Details Screen using locationId.  
*   Related Use Case: UC-05 – Receive Location-Based Notifications.

**Location Content & Audio Management**

*   Create and edit location information.  
*   Manage multilingual guide scripts.  
*   Upload and manage corresponding audio assets.  
*   Save content as DRAFT or submit it as PENDING\_REVIEW.  
*   Related Use Case: UC-06 – Manage Location Content & Audio Assets.

**Analytics & Tourist Feedback Management**

*   Display system usage metrics.  
*   View and filter tourist feedback.  
*   Update feedback status.  
*   Export analytics and feedback reports.  
*  Related Use Case: UC-07 – System Analytics & Tourist Feedback Management.

## **2.2. Out of Scope & Requirements**

* EDITOR/REVIEWER approval and verified package publication are in scope under the UC-06 scope decision below.
* Tourist Application analytics event capture and tourist feedback submission (UC-07 only consumes this data).
* Account-management UI; database authentication and EDITOR/REVIEWER authorization are in scope.
* Auto-loading guide content or auto-playing audio as part of the notification action. Selecting a notification only opens the Location Details Screen; UC-02 then loads the content through its own trigger.   
* Turn-by-turn navigation to a location.  
* User accounts, payments, and social features.

## **2.3. Actors**

The system includes the REVIEWER actor in addition to the existing primary actors. Platform and service dependencies support their interactions as listed below; their responsibilities are detailed in Section 6.4.

| Actor | Type  | Description | Related UC |
| :---- | :---- | :---- | :---- |
| **Tourist** | Primary | Uses the Tourist Application to discover and select locations, access multilingual guide content, play audio, manage offline content, and receive location-based notifications | UC-01, UC-02, UC-03, UC-04, UC-05. |
| **Content Admin (Editor)** | Primary | Creates and edits location information, multilingual scripts, and audio assets; saves drafts, submits content for review. | UC-06 |
| **System Admin / Product Owner.** | Primary | Views system analytics, manages feedback status, and exports reports through the Web Admin Dashboard | UC-07 |
| **Map Service** | Secondary service | Displays the interactive map and markers; offline Map View uses a simplified background without map tiles (online only; offline, locations are browsed through the list) | UC-01 |
| **Platform Location & Geofencing.** | Platform dependency | Provides device coordinates and accuracy and supports geofence registration and entry events | UC-01, UC-05 |
| **Platform Notification** | Platform dependency | Platform dependency. Description: Displays system notifications and opens the Tourist Application using locationId | UC-05 |
| **Platform Audio & Text-to-Speech** | Platform dependency | Provides audio playback, audio-focus handling, and device TTS fallback. | UC-03 |
| **Content Server / CDN** | Infrastructure dependency | Supplies APPROVED guide content, audio assets, and content packages. | UC-02, UC-03, UC-04 |
| **Content Reviewer (REVIEWER)** | Primary | Reviews pending POIs, compares published content, rejects with comments or approves a durable publication job. | UC-06 |
| **Admin Authentication** | Internal component | Database accounts, BCrypt hashes, bearer sessions and EDITOR/REVIEWER checks; account-management UI remains out of scope. | UC-06, UC-07 |

# **Naming aliases:** Content Admin/Editor, Content Administrator, and Administrators/Content Editors refer to Content Admin (Editor). System Administrators/Product Owners refers to System Admin / Product Owner. “Admin” in UC-06 means Content Admin (Editor); “Admin” or “Administrator” in UC-07 means System Admin / Product Owner.

## **2.4. Shared Use Cases and Platform Conditions**

**Product decision confirmed on 19/10/2026:** Tourist Web and Android are two clients of the same product. UC-01–UC-05 describe shared Tourist goals; UC-06–UC-07 remain Web Admin goals. Do not duplicate use cases by platform. Both Tourist clients consume the same approved content through one Backend API and authoritative database/content storage. A shared locationId, languageCode, scriptType, and published content reference identify the same material on both clients; UI source code and device adapters do not have to be shared.

| Use case | Shared requirement | Android conditions | Tourist Web conditions |
| --- | --- | --- | --- |
| UC-01 | Discover/select a location; permission denial preserves manual entry paths. | Native location/map integration; a valid link may open the corresponding location. | A public HTTPS location URL opens the location without installation or login; browser permission handling supports nearby search. |
| UC-02 | APPROVED content, FULL/SHORT, Selected → EN → VI fallback without changing scriptType. | Server or verified local content store. | Same API/content identifiers; verified browser-local content when offline-ready. |
| UC-03 | User-initiated audio, controls, matching TTS fallback, failure handling; no automatic playback from navigation. | Platform audio/TTS and audio-focus lifecycle. | Browser/platform audio/TTS adapters; voice and control behavior must be tested, not assumed identical to Android. |
| UC-04 | Verified packages, explicit download consent, atomic installation, version tracking and Base Content protection. | Base Content is bundled at installation. | Base Content must be provisioned and verified during an online first visit before claiming offline readiness. First-ever access without network is not promised. |
| UC-05 | Detect geofence entry when supported; cooldown, current-language notification/banner, locationId routing, no autoplay. | Native location/geofence/notification permissions and lifecycle. | Position monitoring and foreground banners require an active permitted session. Background/closed-tab monitoring or system notifications must not be promised without verification on the supported browser matrix. |

Unsupported or denied platform capabilities must be explained and handled through the relevant exception/manual path. They must not be silently presented as completed functionality or used to change business rules. Browser/OS versions, supported voices, storage persistence, and foreground/background coverage require a documented acceptance matrix; exact versions and test environments remain \[?\].

**Delivery acceptance:** Deliver one real end-to-end flow first: open/select a location → retrieve APPROVED content → select language and FULL/SHORT → play matching audio or TTS → consume recorded playback data in Web Admin. Demonstrate this flow on both clients against the same backend/content; label any synthetic content or remaining mock behavior. Verify persisted events, consistent reporting, and safe retry rather than relying on static numbers. Analytics capture remains an external integration consumed by UC-07. Complete the other flows progressively under the same PRD; this sequence does not remove offline, notifications, or administration requirements. A demo milestone is not evidence that every use case is finished.

For actual visitor access, Tourist Web must have a reachable HTTPS URL; local-only demo URLs are development/test environments. Existing mock screens may remain as explicit reference/scenario tools while production flows are integrated; they are not acceptance evidence for real persistence, audio, permissions, or package installation.

# **3\. EPIC & PRODUCT REQUIREMENTS**

## **3.1. Epic 1 – Location Discovery & Selection**

### **3.1.1 Objective**

Help Tourists discover and select attractions through nearby search, keyword search, categories, maps, and location-based notifications, including manual selection when location access is unavailable.

### **3.1.2 Features**

* **FR-01**: Nearby search, distance-ascending results, shortlist, radius expansion (UC-01).   
* **FR-02**: Keyword search, category browsing, and map-marker selection (UC-01).   
* **FR-03**: Geofence notifications or In-App Banners, cooldown, and locationId navigation to Location Details (UC-05). 

### **3.1.3 Related Use Cases**

* **UC-01** — Explore & Select Location;   
* **UC-05** — Receive Location-Based Notifications.

## **3.2. Epic 2 – Audio Guide Experience**

### **3.2.1 Objective**

Provide verified multilingual guide text and audio for the selected location with language and fallback.

### **3.2.2 Features**

* **FR-04**: APPROVED content retrieval by locationId, languageCode, and scriptType, with language and fallback (UC-02).   
* **FR-05**: Audio playback controls, and platform-provided TTS fallback when usable audio is unavailable (UC-03).

### **3.2.3 Related Use Cases**

* **UC-02** — Access Multilingual Guide Content; UC-03 — Play Audio Guide.

## **3.3. Epic 3 – Offline Experience**

### **3.3.1 Objective**

Keep essential location data and guide content available offline and allow safe installation and management of optional or updated content.

### **3.3.2 Features**

* **FR-06:** Base Content provisioned under Section 2.4, update checks, and optional-package selection and download of newer approved packages (UC-04).
* **FR-07:** Verified atomic installation of matching location data, scripts, and available audio (UC-04).  
* **FR-08**: Optional-content deletion, cancellation, and physical protection of Base Content (UC-04).

### **3.3.3 Related Use Cases**

* **UC-04** — Manage & Update Offline Content.

## **3.4 Epic 4 – Content Management**

### **3.4.1 Objective**

Enable the Content Admin to maintain location information, multilingual scripts, and corresponding audio before external review and publication.

### **3.4.2 Features**

* **FR-09:** Location listing, localized information, scripts, and audio upload and preview (UC-06).   
* **FR-10:** Save Draft, Submit for Review, rejected-content editing and resubmission, and approved-version protection (UC-06).  
* **FR-11:**   Recording of updatedBy and updatedAt for saved content (UC-06).

### **3.4.3 Related Use Cases**

* ### **UC-06 —** Manage Location Content & Audio Assets.

## **3.5. Epic 5 – Analytics & Tourist Feedback**

### **3.5.1 Objective**

Support service and content improvement through basic usage analytics and management of feedback supplied by external dependencies.

### **3.5.2 Features**

* **FR-12:** Total Playbacks, Top Locations, Language Distribution, and Offline/Online Usage Ratio by reporting period (UC-07).   
* **FR-13**: Feedback listing, detail, filters, pagination, and sequential status updates with RESOLVED final (UC-07).   
* **FR-14**: Period-based .xlsx export, active feedback filters, and retry after errors (UC-07). 

### **3.5.3 Related Use Cases**

* ### **UC-07** — System Analytics & Tourist Feedback Management.

# **4\. DETAILED USE CASE SPECIFICATIONS**

## **4.1. USECASE 1: Explore & Select location**

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-01 |  |
| **UC Name** | **Explore & Select location** |  |
| **Type** | Tourist Application – Web / Android |  |
| **Goal** | The Tourist selects a target attraction in order to view its details and access the multilingual guide content. |  |
| **Primary Actor** | Tourist |  |
| **Secondary Actor** | Map Service, Platform Location & Geofencing |  |
| **Precondition** | The Tourist opens Tourist Web or Android, including through a location-specific URL. Location access may be available for nearby search; manual search and valid location links remain usable without it. |  |
| **PostCondition** | The system determines the locationId, and the application navigates to the **Location Details Screen**. |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|  | **1\.** The Tourist opens the application and selects **“Find Nearby Locations”**, or uses an entry path in A1–A4. | **2\.** The system initiates the nearby-location detection process when nearby search is selected. |
|  |   | **3\.** If location permission has not yet been determined, the system explains why nearby search needs location access and requests permission through the current platform. If permission is granted, continue to step 4; if it is denied or unsupported, follow **E1**. |
|  |  | **4\.** The system retrieves the current latitude, longitude, and accuracy. |
|  |   | **5\.** The system searches for locations within the configured radius 100 to 500 meters and sorts the results by distance in ascending order.**E2** |
|  |  | **6\.** The system displays the locations found within the radius sorted by distance ascending; the \[3–5\] nearest are shown first with their distances. |
|  | **7\.** The Tourist selects the location. | **8\.** The system stores the corresponding locationId and navigates to the **Location Details Screen**. |
| **Alternative Paths** | **A1.** |  |
|  | **1**. selects **“Search by Keyword”** | **2**.The system searches the location database and displays matching results.**E3** |
|  | **3\.** Go to step 7 of the main flow. |  |
|  | **A2** |  |
|  | **1\.**selects **“Map View and browses”** | 2.The system iterates through the markers on the displayed map screen, which supports zooming and panning.  |
|  | **3\.**Go to step 7 of the main flow. |  |
|  | **A3** |  |
|  | **1**.selects “**Browse by Category”** | 2\. The system displays locations belonging to the selected category.**E4** |
|  | **3\.** Go to step 7 of the main flow. |  |
|  | **A4: Open a Location Link / QR URL.** The Tourist opens a URL containing a location reference, for example by scanning a QR code. The system validates the locationId and opens Location Details, which triggers UC-02. Tourist Web must not require installation or login; navigation does not start playback. If the location is unavailable, the system explains the problem and offers manual discovery without opening an unrelated location. |  |
| **Exception Paths** | **E1:** If the Tourist has not allowed location access or the system cannot obtain the current coordinates, the application displays **“Location access is required to automatically find tourist attractions near you”**, with platform-appropriate settings/help where available. The Tourist can continue through keyword search, categories, map selection, or a valid location link. |  |
|  | **E2**: If the GPS coordinates are valid but no locations are found within the current search radius, the system displays the message **“No tourist attractions were found nearby.”** The Tourist can expand the search radius or use **A1**, **A2,A3**. |  |
|  | **E3**: If the keyword entered by the Tourist does not match any location in the database, the system displays an **Empty State** and suggests alternative keywords or popular locations for the Tourist to continue exploring. |  |
|  | **E4**: If no locations are available in the selected category, the system displays an **Empty State** and suggests other categories or popular locations for the Tourist to explore. |  |
| **Extension Points** | N/A |  |
| **Triggers** | **User-Initiated:** The Tourist opens the Tourist Application or navigates to the **Explore / Map** screen. The Tourist selects **“Find Nearby Locations”**, uses the **Search** bar, selects a **Category**, selects a **Marker** on the interactive map, or opens a location-specific URL/QR link (A4). |  |
| **Assumptions** | **Data Assumption:** The server database or verified local content store contains location data, including coordinates, descriptions, categories, and thumbnail images. **Platform Assumption:** Location services may require permission or be unavailable; manual selection remains possible. **Offline Baseline:** Android bundles base location/guide content at installation; Tourist Web provisions and verifies it during initial online use as specified in Section 2.4. UC-04 manages optional/newer packages after that baseline is ready. |  |
| **Reference: Bussiness Rule** | **BR1 – GPS Search Radius:** The location search radius can be configured within a range of **100–500 meters**. The system uses a fixed default value defined in the application configuration (not editable through the Web Admin Dashboard). **BR2 – Result Ranking:** GPS-based search results are sorted by **distance in ascending order**. Keyword- or category-based results may be sorted by **relevance or distance**. **BR3 – Location Permission:** If the Tourist denies location access, the system must not prevent the Tourist from continuing to use the application. GPS-dependent features are disabled, and the system must provide alternative location-selection methods, such as **Search by Keyword**, **Browse by Category**, or **Map View**. |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC01\_activity.drawio |  |

## **4.2. USECASE 2: Access Multilingual Guide Content**

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-02 |  |
| **UC Name** | **Access Multilingual Guide Content** |  |
| **Type** | Tourist Application – Web / Android |  |
| **Goal** | Retrieve and display approved guide content according to the Tourist's selected language and duration mode. |  |
| **Primary Actor** | Tourist |  |
| **Secondary Actor** | Content Server / CDN |  |
| **Precondition** | The application has navigated to the Location Details Screen through UC-01, UC-05, or another valid navigation path, and a valid locationId is available. |  |
| **Postcondition** | The guide content (scriptText) is successfully loaded and ready to be displayed or used by UC-03. |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|  | **1\.** The Tourist opens the Location Details Screen through a valid navigation path. | The system receives the locationId, current languageCode, and default scriptType (FULL). |
|  |   | **2\.** The system retrieves content matching (locationId, languageCode, scriptType) from the available data source. |
|  |   | **3\.** The system verifies that the retrieved content has status \= APPROVED before it is loaded. |
|  |   | **4\.** The system loads the corresponding approved scriptText. |
|  |                              | **5\.** The application displays the scriptText on the **Location Details Screen**. |
| **Alternative Paths** | **A1:** The Tourist selects another language from the Language Picker. The system retrieves the content using the new *languageCode* while retaining the same *locationId* and *scriptType*, and then updates the guide content displayed on the screen. |  |
|  | **A2:**The Tourist selects **Short Summary** mode. The system retrieves the corresponding content with *scriptType \= SHORT* for the current location and language, and then updates the displayed guide content. |  |
| **Exception Paths** | **E1:** If approved guide content is unavailable in the selected language, the system informs the Tourist that the translation is currently unavailable and applies the fallback-language rule. The system attempts **APPROVED** content in **English (EN)**; if English is also unavailable, it attempts **APPROVED** content in **Vietnamese (VI)**. |  |
|  | **E2**: If no approved guide content is available for the selected location, the system displays **“Content for this location is currently being updated.”** The **Listen to Guide** function is unavailable. |  |
| **Extension Points** | N/A |  |
| **Triggers** | **System-Initiated:** After a valid navigation to the Location Details Screen, including UC-01 selection or UC-05 notification/banner selection, the system triggers UC-02 using the current language and default FULL mode. **User-Initiated:** When the Tourist changes the **Language** or **Duration Mode** (FULL / SHORT), the system re-triggers UC-02 to load the corresponding content. |  |
| **Assumptions** | **Content Data:** Each location may have one or more guide scripts for different languages and content types (FULL, SHORT). **Supported Languages:** The system has a predefined list of supported languages, and each language uses a valid languageCode.  **Data Source Availability:** When online, the system can retrieve approved content from the API/Server; when offline, the required approved content is available in verified Base Content or an approved package installed through UC-04, under Section 2.4. |  |
| **Reference: Bussiness Rule** | **BR1 – Content Approval:** Only guide content with status \= APPROVED may be provided to the Tourist. **BR2 – Language Fallback:** The language fallback priority is: Selected Language → English (EN) → Vietnamese (VI) |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC02\_activity.drawio |  |

## **4.3. USECASE 3: Play Audio Guide**

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-03 |  |
| **UC Name** | **Play Audio Guide** |  |
| **Type** | Tourist Application – Web / Android |  |
| **Goal** | Allow the Tourist to listen to approved guide content for the selected location, effective language, and content mode, using pre-rendered audio or platform-provided Text-to-Speech (TTS) when that audio is unavailable or cannot be played. |  |
| **Primary Actor** | Tourist |  |
| **Secondary Actor** | Platform Audio & Text-to-Speech, Content Server / CDN |  |
| **Precondition** | UC-02 has successfully loaded APPROVED guide content; valid locationId, effective languageCode after any UC-02 language fallback, scriptType, and non-empty scriptText are available. The availability of a compatible TTS engine and voice is checked when the TTS fallback is needed; it is not a prerequisite for reading the text or playing available audio. |  |
| **Postcondition** | **Success:** the approved guide content is played using matching pre-rendered audio or TTS, and the application identifies the active playback source. The current playback state and position are maintained within the active playback session. **Otherwise:** if neither audio nor TTS can be used (E3), no playback is reported as successful; the loaded text and current location, language, and content mode remain available for reading or retry. |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|  | **1\.**  The Tourist selects **Play Audio**. | The system receives a request to play the guide content for the current location. |
|  |   | **2\.** The system identifies the corresponding audio using locationId, effective languageCode, and scriptType provided by UC-02. |
|  |   | **3\.** The system checks whether the corresponding audio file is available from the current data source. If it is unavailable, follow E1. |
|  |   | **4\.** The system loads the audio file and prepares the playback session. If loading or preparation fails, follow E1. |
|  |   | **5\.** The system starts audio playback. |
|  |   | **6\.** Once playback starts, the application updates the state to **Playing**, identifies the source as pre-rendered audio, and displays playback controls. |
|  | **7\.** The Tourist continues listening to the guide. | The system maintains the current playback state and position until playback completes or another action/event occurs. |
| **Alternative Paths** | **A1: Playback Controls.** While pre-rendered audio or TTS is playing, the Tourist may Pause, Resume, seek forward/backward, or change the playback speed. The system updates the active playback session and continues from the corresponding position according to the selected action. |  |
|  | **A2: Play Guide Using Text-to-Speech.** When E1 occurs: **1.** The system informs the Tourist that pre-rendered audio is unavailable and TTS will be used. **2.** The system initializes the platform-provided TTS service and checks for a usable voice matching the effective languageCode returned by UC-02. When offline, that voice must operate without a network connection. If the service or suitable voice is unavailable, follow E3. **3.** The system prepares speech from the exact approved scriptText for the current locationId and scriptType; it does not translate, summarize, or generate new guide content. **4.** The system starts TTS playback. Once speech starts, it displays **Playing — Text-to-Speech** and the playback controls from A1. **5.** The system maintains the TTS playback state and position until completion or another action/event; audio-focus interruptions follow E2. |  |
| **Exception Paths** | **E1: Audio Unavailable or Playback Failed.** If the matching audio file cannot be found, loaded, prepared, or played, including a failure during playback, the system stops/releases the failed audio session and follows A2 using the guide content already loaded by UC-02. It must not play the failed audio and TTS simultaneously. |  |
|  | **E2: Audio Focus Interrupted.** If a higher-priority event, such as an incoming phone call or another application, takes control of the audio output, the system pauses the active audio or TTS session and stores its current playback position. When audio focus is restored, playback may continue from the stored position. |  |
|  | **E3: TTS Unavailable or Failed.** If the TTS engine cannot initialize, no usable voice matches the effective languageCode, the required voice needs an unavailable network connection, or speech synthesis/playback fails, the system ends the preparation/playback attempt and displays an error explaining that the guide cannot currently be read aloud. It preserves scriptText and the current location, language, and content mode, and allows the Tourist to retry after the cause is resolved. It does not remain indefinitely in Loading, report successful playback, silently use a voice for another language, or change the approved content. |  |
| **Extension Points** | N/A |  |
| **Triggers** | **User-Initiated:** The Tourist selects **Play Audio** on the Location Details Screen after UC-02 has loaded approved guide content. |  |
| **Assumptions** | Each guide script may have an optional corresponding audio file for each languageCode and scriptType. The active platform supplies audio/TTS capabilities and interruption/lifecycle handling through its adapter. Engines, voices, controls, and network requirements vary by device/browser and must be verified. Offline playback requires matching local audio from verified Base Content or a package installed through UC-04, or locally available scriptText with a usable offline voice for its effective languageCode. Text alone does not guarantee offline speech. Section 2.4 defines baseline provisioning; supported language/voice coverage and test environments require team confirmation. |  |
| **Reference: Bussiness Rule** | **BR1 – Playback Content Consistency:** Audio and TTS must correspond to the current locationId, effective languageCode, and scriptType provided by UC-02. **BR2 – Pre-rendered Audio Priority:** Use matching valid pre-rendered audio when it can be played; use platform-provided TTS only as a fallback when that audio is unavailable or fails. **BR3 – Approved Text and Voice Consistency:** TTS reads the exact APPROVED scriptText loaded by UC-02 with a voice matching its effective languageCode. UC-02 owns language fallback; UC-03 must not independently translate, summarize, or substitute another language or script type. **BR4 – Offline TTS Availability:** TTS may be offered offline only when the required text and a compatible voice that works without a network connection are available; otherwise follow E3. **BR5 – Playback Source and Trigger:** Identify whether audio or TTS is active, allow only one source to play at a time, and start playback only in response to the Tourist's Play request or its fallback path. Opening Location Details or selecting a notification must not itself start audio or TTS. |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC03\_activity.drawio |  |

## **4.4. USECASE 4: Manage & Update Offline Content**

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-04 |  |
| **UC Name** | **Manage & Update Offline Content** |  |
| **Type** | Tourist Application – Web / Android |  |
| **Goal** | Allow the Tourist to update, add, or remove offline content while preserving verified Base Content, so guide text and audio remain usable without a network connection after the baseline is ready. Android bundles the baseline; Tourist Web provisions it during initial online use (Section 2.4). |  |
| **Primary Actor** | **Tourist** |  |
| **Secondary Actor** | N/A |  |
| **Precondition** | Verified Base Content is ready in the platform's local content store under Section 2.4. A network connection is needed for initial Web provisioning and for checking/downloading updates; deleting optional content does not require one. If the baseline is not ready, explain that initial online preparation is required instead of reporting Offline Ready. |  |
| **Postcondition** | **Success:** the offline content is updated and shown as Up to Date / Offline Ready, or the selected optional content is removed and the content status is updated. **Otherwise (no update, cancellation, or failure):** the currently installed offline content remains unchanged. |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|  | **1\.** The Tourist opens Offline Content Management. | The system displays the currently installed content and its version status. |
|  | **2\.** The Tourist selects Check for Updates or Update. | The system checks the Server for a newer **APPROVED** content package version. |
|  |   | **3**. If a newer approved package is available, the system displays the update information (new or updated content and required size). |
|  | **4**. The Tourist confirms the update. | The system checks the required storage against the available device storage. If storage is sufficient, the system downloads the new content and displays the download progress. |
|  |   | **5**. After the download is completed, the system verifies the integrity of the downloaded content. |
|  |   | **6\.** If the approved package is valid, the system atomically replaces the affected local scripts and audio assets with the verified package and stores its new version. |
|  |   | **7**. The system displays the status Up to Date / Offline Ready. |
| **Alternative Paths** | **A1: No Update Available.** At step 3, if no newer version is available, the system informs the Tourist that the current content is already up to date. The use case ends and nothing is changed. |  |
|  | **A2: Delete Optional Offline Content.** The Tourist selects Delete Content. The system displays the additionally downloaded content and asks the Tourist to select the items to delete and to confirm. After the Tourist confirms, the system removes the selected content from local storage and updates the content status. The protected Base Content is never removed by this operation (**BR-01**). If the Tourist cancels, nothing is changed. |  |
|  | **A3: Tourist Cancels the Update.** At step 4, if the Tourist does not confirm the update, the system keeps the current content unchanged and the use case ends. |  |
| **Exception Paths** | **E1: Insufficient Storage.** If the device does not have enough available storage for the update, the system stops the operation, makes no changes, and informs the Tourist of the required storage. The system may offer to open Offline Content Management so the Tourist can delete optional content. |  |
|  | **E2: Network Lost.** If the Internet connection is lost during the download, the system stops the download and discards the incomplete data. The Tourist may retry later, when a connection is available, by selecting Update again. The currently installed content remains unchanged. |  |
|  | **E3: Invalid Update Package.** If the downloaded content fails integrity verification or cannot be installed, the system discards the invalid data, preserves the current offline version, and informs the Tourist to try again. |  |
| **Extension Points** | N/A |  |
| **Triggers** | **User-Initiated:** The Tourist opens the Offline Content / Updates section and selects Check for Updates, Update, or Delete Content. **System-Initiated:** The system detects a newer content version and displays Update Available. The download starts only after the Tourist confirms. |  |
| **Assumptions** | Android's bundled baseline supports offline access without a prior content download. Tourist Web needs an online first visit to provision and verify the same baseline before offline access. The Server supplies versioned, published, approved packages. The platform's local content store holds content and installed versions. Browser storage availability/persistence must be tested; if storage is absent or externally cleared, report the actual readiness state and require online preparation again rather than claiming content still exists. Application-controlled deletion must preserve Base Content. |  |
| **Reference: Business Rule** | **BR-01 – Base Content Protection:** Verified Base Content must not be removed by application-controlled offline content management, under Section 2.4. **BR-02 – Safe Update:** Only a successfully verified **APPROVED** package may replace existing content. If an update fails, the currently installed content must not be overwritten or removed. **BR-03 – Version Management:** The system must store the local content version for comparison with the Server version. **BR-04 – Atomic Package Installation:** A package version must install its matching guide scripts and audio assets together; the Tourist Application must not use a partial or DRAFT/PENDING\_REVIEW package. |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC04\_activity.drawio |  |

## **4.5. USECASE 5: Receive Location-Based Notifications**

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-05 |  |
| **UC Name** | **Receive Location-Based Notifications** |  |
| **Type** | Tourist Application – Web / Android |  |
| **Goal** | Notify the Tourist when entering the area of a tourist location and allow quick access to the corresponding location information. |  |
| **Primary Actor** | Tourist |  |
| **Secondary Actor** | Platform Location & Geofencing, Platform Notification |  |
| **Precondition** | **1.** Tourist Web or Android is running in a platform/session capable of location monitoring under Section 2.4. **2.** The verified local content store contains coordinates, geofence radius, locationId, and localized names. Location/notification permissions may still be unavailable; handle these through E1/E2. Unsupported or suspended background monitoring must not be represented as an active service. |  |
| **Postcondition** | **Success:** a location-based notification (or In-App Banner) is displayed to the Tourist. If the Tourist selects it, the application opens the corresponding Location Details Screen using the associated locationId; UC-02 is then triggered by its own trigger to load the guide content.   **Otherwise:** no location-based notification is displayed (E1, E2, A3) or no navigation is performed (A2). |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|   | **1\.** The Tourist moves into the configured geofence of a tourist location. | The system detects the geofence entry event. |
|   |   | **2\.** The system identifies the corresponding location using the geofence data and locationId. |
|   |   | **3\.** The system checks whether the notification cooldown period for the location has expired. |
|   |   | **4\.** If notification delivery is allowed, the system creates and displays a location-based notification, containing the locationId, using the current application language. |
|   | **5\.** The Tourist selects the notification. | The system reads the associated locationId. |
|   |   | **6\.** The application opens the corresponding Location Details Screen and triggers UC-02 using the associated locationId. |
| **Alternative Paths** | **A1: Application in Foreground.** If the application is in the foreground when the geofence event occurs, the system displays an In-App Banner, containing the locationId, instead of an operating-system notification. If the Tourist selects the banner, the application opens the corresponding Location Details Screen using the associated locationId. |  |
|  | **A2: Notification Not Selected.** If the Tourist does not select the displayed notification or banner, the system performs no navigation. The Tourist may continue using the application normally. |  |
|  | **A3: Notification Cooldown Active.** At step 3, if the cooldown period for the location has not expired, the system does not display a notification (BR1). The Tourist may continue using the application normally. |  |
| **Exception Paths** | **E1: Location Permission Unavailable.** If the location permission is disabled, revoked, or unavailable, the system cannot monitor geofence events and does not generate location-based notifications. The Tourist can still manually explore and select locations using the functions of UC-01. |  |
|  | **E2: Notification Permission Unavailable.** If notification permission or platform support is unavailable, the system cannot display a system notification. If the application is active and permitted location monitoring is available, it may display an In-App Banner (A1). |  |
| **Extension Points** | N/A |  |
| **Triggers** | **System-Initiated:** The device enters the configured geofence of a tourist location. **User-Initiated:** After receiving the notification, the Tourist selects it to open the corresponding location. |  |
| **Assumptions** | The Local Database contains verified location information: locationId, latitude, longitude, radius, and localized location name. Geofence monitoring can operate using location data already available on the device. Each monitored location has a valid configured geofence radius. The application can open a Location Details Screen using a valid locationId.  MVP scope: selecting a notification only opens the Location Details Screen; the notification itself does not load content or play audio. After the Location Details Screen opens, UC-02 is triggered by its own System-Initiated trigger, and audio playback remains a Tourist-initiated action in UC-03.  |  |
| **Reference: Business Rule** | **BR1 – Notification Cooldown:** Each location may generate at most one notification within the configured cooldown period to prevent repeated notifications. **BR2 – Location ID Routing:** Every notification or In-App Banner must contain the corresponding locationId. When the Tourist selects it, the application must use this value to open the correct location. **BR3 – Notification Language:** The title and message of every notification and In-App Banner must use the current language configured in the application. **BR4 – Verified Location Data:** Location-based notifications must use location names and information stored in the verified Local Database. |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC05\_activity.drawio. |  |

## **4.6. USECASE 6: Manage Location Content & Audio Assets**

**Scope decision — 25 October 2026 (supersedes the former external-approval/authentication boundary):** Web Admin has EDITOR and REVIEWER roles authenticated by database accounts with BCrypt password hashes and revocable bearer sessions. EDITOR creates/edits POIs, manually enters names and FULL/SHORT scripts or translates source text to selected configured languages, edits translated text, uploads audio, saves incomplete DRAFTs and submits PENDING_REVIEW. Field provenance is MANUAL, AUTO_TRANSLATED or EDITED_AFTER_TRANSLATE. Coordinates/radius are entered and confirmed manually. Translation does not generate audio; provider failure preserves entered data and manual entry.

Additional scope decision — 25 October 2026: EDITOR uploads a shared POI cover image (JPEG/PNG), previews and saves it with the draft; publication exposes the approved image atomically with the POI. The form explicitly offers manual entry or automatic translation. Client-side TTS reads approved text when no prerecorded audio is supplied; uploading audio remains optional.

REVIEWER compares pending and current public content, approves or rejects with a mandatory rejection comment. States: DRAFT → PENDING_REVIEW → APPROVED or REJECTED; REJECTED can be edited/resubmitted. Approval persists a resumable database publication job. Promotion and verified package publication commit atomically; only then the workspace becomes PUBLISHED. BUILDING/READY/FAILED package versions retain checksum, signature, byte size, timestamps and errors. Failure preserves previous public rows and READY packages and permits retry. Historical content IDs remain available for offline playback receipts. Tourist Web/Android poll the manifest on opening/foreground, reconnection and periodically while active, with ETag/304; users choose downloads and installation waits for playback to end. Push while the app is closed and account-management UI remain outside scope. Older diagrams describing an external approver/authenticator require synchronization with this decision; they do not override it.

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-06 |  |
| **UC Name** | **Manage Location Content & Audio Assets** |  |
| **Type** | Web Admin Dashboard |  |
| **Goal** | Allow the Content Admin to create or edit location information, multilingual guide content, and the corresponding audio assets, and save them as a draft or submit them for review. |  |
| **Primary Actor** | Content Admin / EDITOR and Content Reviewer / REVIEWER |  |
| **Secondary Actor** | None |  |
| **Precondition** | The Admin is authenticated and authorized to manage content. |  |
| **Postcondition** | **Success:** the location data, guide content, and audio assets are saved with DRAFT or PENDING\_REVIEW status. When an approved version is edited, the current published version remains available to the Tourist Application while the replacement waits for review. **Otherwise:** nothing is submitted. The Admin remains on the form with the entered data preserved (E1, E2). |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|  | **1**. The Admin selects an existing location or chooses Add New Location. | **2**. The system opens the corresponding form. |
|  | **3**. The Admin enters or edits localized location names for each supported language, category, GPS coordinates, and geofence radius. | **4**. The system records the entered information. |
|  | **5**. The Admin enters or edits guide content for the supported languages and for each script type (FULL or SHORT). | **6**. The system associates each script with its locationId, languageCode, and scriptType. |
|  | **7\.** The Admin uploads the corresponding audio asset for each language and script type. | **8**. The system validates the file format and stores the audio asset. |
|  | **9**. The Admin previews the audio and reviews the entered content.  | **10**. The system provides an audio preview and displays the entered data. |
|  | **11\.** The Admin selects Submit for Review. | **12\.** The system validates all required fields. |
|  |   | **13\.** If the data is valid, the system saves the content and changes its status to PENDING\_REVIEW. If an approved version exists, the system preserves that published version until the internal REVIEWER publication job completes successfully. |
| **Alternative Paths** | **A1: Save as Draft.** If the content is incomplete, the Admin selects Save Draft instead of Submit for Review. The system saves the data with DRAFT status without requiring all mandatory fields. The content is not available to the Tourist Application. |  |
|  | **A2: Edit Existing Location.** At step 1, the Admin selects an existing location. The system loads the existing data into the form, and the Admin edits one or more items such as the name, coordinates, guide content, or audio. The Admin may also replace the audio of a single language only; the system then updates that audio asset and preserves all other data. Changes are saved as DRAFT or submitted as PENDING\_REVIEW (steps 11–13, A1), and the published version remains protected (BR4). Content returned with REJECTED status is edited in the same way and then saved as a draft or resubmitted for review. |  |
| **Exception Paths** | **E1: Invalid Audio File or Upload Failed.** At step 8, if the audio file uses an unsupported format, cannot be processed, or the upload fails, the system rejects the file, displays an error, and preserves the current form data. The Admin selects another file or retries the upload. |  |
|  | **E2: Missing Required Location Data.** At step 12, if required information such as the location name, category, latitude, longitude, or geofence radius is missing, the system prevents submission and highlights the fields to be completed. The Admin returns to the form. |  |
| **Extension Points** | N/A |  |
| **Triggers** | **User-Initiated:** The Admin opens Location Management and chooses to create or edit a location. |  |
| **Assumptions** | The Web Admin has an Internet connection. The system supports storing guide text and audio assets. The Admin prepares the guide content and audio before or during editing. Supported languages and location categories have already been configured in the system. REVIEWER approval and verified package publication are included in this use case under the scope decision above. |  |
| **Reference: Business Rule** | **BR1 – Required Location Information:** To be submitted for review, each location must contain at least a valid name, category, GPS coordinates, and geofence radius. A DRAFT may be saved with incomplete data. **BR2 – Content Status:** EDITOR saves DRAFT/PENDING_REVIEW; REVIEWER decides APPROVED/REJECTED, and successful package publication assigns PUBLISHED. Public content remains APPROVED for backward API compatibility. **BR3 – Content Mapping:** Each script and audio asset must be associated with at least a locationId, languageCode, scriptType so that UC-02 and UC-03 can identify matching content and audio; the package version is assigned later by the external publication dependency. **BR4 – Approved Content Protection:** Editing previously approved content must not remove the version currently available to the Tourist Application until the replacement has been approved and published. |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC06\_activity.drawio |  |

## **4.7. USECASE 7: System Analytics & Tourist Feedback Management**

| Attributes | Description |  |
| :---- | :---- | :---- |
| **UC ID** | UC-07 |  |
| **UC Name** | **System Analytics & Tourist Feedback Management** |  |
| **Type** | Web Admin Dashboard |  |
| **Goal** | Allow the Administrator to monitor key system usage metrics and manage feedback, ratings, and suggestions submitted by tourists, to support improvements to content and services. |  |
| **Primary Actor** | System Admin / Product Owner |  |
| **Secondary Actor** | None |  |
| **Precondition** | The Admin is authenticated and authorized to access the Web Admin Dashboard. |  |
| **Postcondition** | **Success:** the analytics data is displayed, and any feedback status change made by the Admin is saved together with its update timestamp. **Otherwise:** if no data is available (E1) or the export fails (E2), the system shows an Empty State or an error message and no stored data is changed. |  |
| **Basic Course of Event** | **Actor Action** | **System Response** |
|  | **1\.** The Admin opens Analytics & Feedback. | The system loads the data of the last 30 days. |
|  |   | **2\.** The system displays Total Playbacks, Top Locations, Language Distribution, and Offline/Online Usage Ratio. |
|  | **3\.** The Admin opens the feedback list. | The system displays the recent feedback records with category, content, rating, and status. |
|  | **4\.** The Admin selects a feedback record. | The system displays the feedback details. |
|  | **5\.** The Admin updates the feedback status. | The system saves the new status and the update timestamp. |
| **Alternative Paths** | **A1: Change Reporting Period.** After step 2, the Admin selects a different reporting period. The system reloads the analytics data for the selected period. |  |
|  | **A2: Filter Feedback.** After step 3, the Admin filters the feedback by category or status. The system displays only the feedback records that match the selected criteria. |  |
|  | **A3: Export Report.** The Admin selects Export Report. The system generates an .xlsx file containing the analytics and feedback data for the current reporting period (applying any active feedback filters) and makes it available for download. |  |
| **Exception Paths** | **E1: No Data Available.** If no data matches the selected reporting period or filter, the system displays an Empty State instead of charts or records. |  |
|   | **E2: Export Failed.** At A3, if the report cannot be generated, the system displays an error message and allows the Admin to retry. |  |
| **Extension Points** | N/A |  |
| **Triggers** | **User-Initiated:** The Admin selects Analytics & Feedback on the Web Admin Dashboard. |  |
| **Assumptions** | The Tourist Application records the usage events required for analytics through an external analytics-event capture dependency. Tourist feedback is submitted and stored through an external feedback-submission dependency. The system contains sufficient data to calculate the required basic metrics. The Admin is allowed to download reports when using the Export function. Filtering, exporting, and updating status are behaviors inside this use case and are not separate use cases. |  |
| **Reference: Business Rule** | **BR1 – Access Control:** Only authorized Admin users may access analytics data and manage feedback. **BR2 – Feedback Status:** Each feedback record must have one valid status: NEW, IN\_REVIEW, or RESOLVED. A record progresses in this order: NEW, then IN\_REVIEW, then RESOLVED. **BR3 – Default Reporting Period:** The Dashboard displays the data of the last 30 days by default. |  |
| **Author(s)** |   |  |
| **Date** |   |  |
| **Activities Diagram** | See UC07\_activity.drawio |  |

 

# **5\. DATA MODELS**

***Question:** What data does the system need to store to correctly perform 7 use cases?* 

## **5.1 Entity Relationship Model**

**Proposed Entity Relationship Model:** Location references Category; LocationTranslation references Location and Language; GuideScript references Location and Language; AudioAsset references GuideScript; a published GuideScript references ContentPackage by packageVersion; Feedback may reference Location. AnalyticsEvent stores location and language identifiers for the external analytics data consumed by UC-07.   
**ERD link**: \[\].  
Package versions are assigned at publication time. Unpublished DRAFT or PENDING\_REVIEW scripts do not require packageVersion. The manifest associates the affected location data, scripts, and available audio with their package version for atomic installation. Package-version uniqueness requires team confirmation before finalizing database keys and cardinalities.

## **5.2 Entity Definitions**

**Proposed Entity Definitions:** The following records are derived from the existing use cases and the data-model worksheet. They describe required information and external references; they do not add account-management, configuration-management, feedback-submission, or event-capture use cases.

| Entity | Fields | Used by  | Purpose |
| :---- | :---- | :---- | :---- |
| **Location** | locationId (PK), categoryId (FK), latitude, longitude, geofenceRadius, thumbnailUrl, status | UC-01,  UC-04,  UC-05,  UC-06. | Location information (including its translations) and its content status; GuideScript.status separately tracks guide content. |
| **Location Translation.** | locationId (FK),  languageCode (FK),  name,  description. | UC-01,  UC-04,  UC-05,  UC-06 | Localized names and optional descriptions. Proposed submission rule: at least one name in VI or EN; Save Draft may be incomplete. |
| **Category** | categoryId (PK),  localized name,  isActive. | UC-01,  UC-06 | The fixed MVP category catalog; catalog entries and their localized values are \[\]. No category-management use case is added. |
| **Language** | languageCode (PK),  name | UC-02,  UC-03,  UC-06 | The fixed MVP language catalog. Support is not assumed solely because a fallback language is named. |
| **GuideScript** | scriptId (PK),  locationId (FK),  languageCode (FK),  scriptType (FULL or  SHORT), scriptText,  status,  packageVersion (not  required before publication),  reviewerComment,  updatedBy,  updatedAt. | UC-02,  UC-03,  UC-04,  UC-06. | The guide text for a location, language, and script type, with external review information and published-package association. |
| **AudioAsset** | audioId (PK),  scriptId (FK),  fileUrl,  format,  durationSec,  sizeBytes, checksum | UC-03,  UC-04,  UC-06 | Optional audio linked to the script's locationId, languageCode, and scriptType. Proposed limits are defined in NFR-14. |
| **Content Package** | packageVersion (PK, subject to uniqueness confirmation),  sizeBytes,  checksum,  publishedAt,  manifest. | UC \- 04  | Verified atomic installation of affected location data, scripts, available audio, and removed or archived-item visibility changes. |
| **Local Content Version** | installedPackageVersion,  installedAt,  isBase. | UC-04 | Track installed content and distinguish protected Base Content from Optional Content. |
| **Feedback** | feedbackId (proposed PK),  locationId (nullable FK),  category,  rating (proposed \[1–5\]),  content,  languageCode,  status (NEW, IN\_REVIEW, RESOLVED),  createdAt,  updatedAt. | UC-07 | Supplied through the external feedback-submission dependency; RESOLVED is final. |
| **Analytics Event** | eventId (proposed PK), eventType,  locationId,  languageCode, connectivity,  occurredAt. | UC-07 | Supplied through external Analytics Event Capture. The event-contract field timestamp denotes the occurrence time stored as occurredAt, not an additional event time. Event-type applicability is defined in the Appendix. |

Audit information is limited to updatedBy and updatedAt on saved content; no content-status history, additional audit entity, or database structure is assumed here.

# **6\. SYSTEM ARCHITECTURE**

## **6.1 Class Diagram**

**Proposed Class Diagram**: The domain classes follow the entity definitions in Section 5.2: Location, LocationTranslation, Category, Language, GuideScript, AudioAsset, ContentPackage, Feedback, and AnalyticsEvent. LocalContentVersion represents device content-version information. Attributes follow Section 5.2, and associations follow the references in Section 5.1. Operations are derived from UC-01 through UC-07; implementation-specific method signatures and unconfirmed multiplicities are not specified.

## **6.2 Component Diagram**

**Proposed Component Diagram:** Tourist Web and Android each contain a presentation layer and platform-specific adapters for Content Service, Audio Player/TTS, location/notifications, Offline Manager, and Local Content Store. Both follow the shared UC-01–UC-05 rules and use the same Backend API and published content identifiers. Online content comes from the backend/content storage; offline content comes from the verified local store under Section 2.4. Offline Manager verifies and atomically installs approved packages. Web Admin is a separate authorized client for UC-06/UC-07. Backend API uses the authoritative Server Database and Object Storage / CDN. External Content Publication Service performs review, version assignment, publication, and ARCHIVED transitions. Admin Authentication, Analytics Event Capture, and Feedback Submission retain their external boundaries; sharing a backend does not grant Tourist clients access to Admin operations.

## **6.3 Deployment Diagram**

**Proposed Deployment Diagram:** Android runs on a supported device with native services and local storage; Tourist Web runs in a supported browser with browser/platform adapters and local storage. Both clients and the separate Web Admin access one Backend API over HTTPS. The API connects to the authoritative Server Database and content/audio/package storage. Tourist Web is served from a visitor-accessible HTTPS URL; a development-only LAN address does not establish public availability. External publication/authentication boundaries remain as defined in Section 6.4. Analytics capture on either Tourist client uploads events through the same agreed event contract; feedback submission remains an external integration. Hosting providers, supported browser versions, capacities, and deployment allocation remain \[?\].

## **6.4 External Services & Dependencies**

Platform services below denote native OS capabilities on Android and browser/platform capabilities on Tourist Web, subject to Section 2.4. The system relies on the following services that are outside the scope of the use cases in this document. Fixed MVP configuration, storage, and content distribution support UC-01 through UC-07 as infrastructure only; no additional epic, feature, or use case is defined for them.

| Service | Type | Role | Related UC |
| :---- | :---- | :---- | :---- |
| **Map Service** | External | Displays the interactive map and location markers. | UC-01 |
| **Platform Location & Geofencing** | Platform | Provides the device position and accuracy, and geofence entry events. | UC-01, UC-05 |
| **Platform Notification** | Platform | Displays notifications and opens the app with a locationId. | UC-05 |
| **Platform Audio & Text-to-Speech** | Platform | Plays audio, manages audio focus, and reads scriptText when audio is unavailable. | UC-03 |
| **Content Server / CDN** | Infrastructure dependency | Stores and delivers approved guide content, audio files and offline packages. | UC-02, UC-03, UC-04 |
| **Content Publication Service** | External | Receives content submitted for review, supports review (APPROVED / REJECTED), assigns a package version, and publishes packages to the Content Server/CDN. | UC-04, UC-06 |
| **Admin Authentication** | External | Authenticates and authorizes Web Admin users (account management is out of scope). | UC-06, UC-07 |

#### **6.4.1 Content Lifecycle** 

| Status | Meaning | Set by | Visible to Tourist Application |
| :---- | :---- | :---- | :---- |
| **DRAFT** | Content is being prepared. | Content Admin (UC-06) | No |
| **PENDING\_REVIEW** | Submitted and waiting for review. | Content Admin (UC-06) | No (the previously published version remains available) |
| **REJECTED** | Returned by the reviewer with a comment. | Content Publication Service | No |
| **APPROVED** | Approved and included in a published package version. | Content Publication Service | Yes |
| **ARCHIVED** | No longer used; removed from the app in the next package. | Content Publication Service | No |

Flow: DRAFT → PENDING\_REVIEW → APPROVED, or → REJECTED → DRAFT (after editing).

UC-06 only creates DRAFT and PENDING\_REVIEW. Every other change of status is performed by the Content Publication Service.

# **7\. NON-FUNCTIONAL REQUIREMENTS**

**Proposed Non-Functional Requirements:** Bracketed values are initial targets requiring team confirmation and validation. Test devices, network conditions, workload, measurement methods, and acceptance evidence are \[?\]. NFR-08, NFR-10, and NFR-12, as well as the Decree 13/2023/ND-CP compliance statement in NFR-07, are desirable targets and are not MVP acceptance criteria.

* **NFR-01 — Performance**: Location Details Screen loads within \[2\] s on 4G and within \[1\] s from the Local Content Store.  
* **NFR-02 — Performance**: Nearby-location search returns results within \[3\] s after a valid GPS fix.  
* **NFR-03 — Performance:** Pre-rendered audio starts within \[2\] s of selecting Play; TTS fallback starts within \[3\] s when a device voice for the effective languageCode is available.  
* **NFR-04 — Offline**: Base Content is at most \[150\] MB. Once the baseline is verified and available locally, UC-01 search/category browsing, UC-02, and UC-03 work without network access. Android bundles the baseline; Tourist Web requires initial online provisioning and a verified supported storage environment. The application must show its actual offline readiness. Map View requires network; offline, use the list. Playback needs local audio or compatible offline TTS, subject to UC-03 E1/E3 and Section 2.4.
* **NFR-05 — Platform:** The agreed Tourist clients are Android and Web browsers. Android minimum version is proposed as \[8.0+\]; supported browser/device versions and capability coverage are \[?\]. Native iOS is not required for this delivery. Shared use cases require per-platform acceptance evidence, particularly audio/TTS, offline storage, permissions, and foreground/background behavior.
* **NFR-06 — Security:** All client–server traffic uses HTTPS (TLS). Web Admin requires authenticated, role-based access. Content packages are verified by checksum and signature under UC-04 BR-02.  
* **NFR-07 — Privacy:** Location is used only for nearby search and geofencing; analytics events use anonymous identifiers. Proposed compliance requirement: data handling complies with Decree 13/2023/ND-CP on personal data protection, subject to confirmation by a qualified reviewer. This is a requirement for review, not evidence of established compliance.  
* **NFR-08 — Battery:** Geofence monitoring consumes at most \[3\]% battery per hour of normal walking use. Measurement device and conditions are \[nhóm điền\].  
* **NFR-09 — Reliability:** A failed or interrupted update never corrupts or removes installed content, as required by UC-04 BR-02.  
* **NFR-10 — Accessibility:** Android supports system font scaling and screen-reader labels; Tourist Web supports browser zoom, keyboard access, and screen-reader labels. Both target WCAG 2.1 AA contrast.
* **NFR-11 — Localization:** UI and guide content use Unicode; the supported languages and support flags are listed in the Appendix.  
* **NFR-12 — Availability:** Server API availability is at least \[99.5\]% per month.  
* **NFR-13 — Scalability:** The system supports at least \[500\] locations and \[1,000\] concurrent users.  
* **NFR-14 — Media:** Audio assets use \[MP3 or M4A (AAC)\] at a bitrate of \[64–128\] kbps. Proposed upload size and duration limits remain those in UC-06 BR7.

# **8\. APPENDIX**

This Appendix defines naming aliases, fixed MVP configuration, data contracts, permissions, scope traceability, risks, and interface references for the existing seven use cases. Bracketed proposals and placeholders require team confirmation or completion.  
**8.1 Terminology and Naming Aliases**  
