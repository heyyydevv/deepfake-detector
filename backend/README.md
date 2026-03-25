# VeraLens — Deepfake Image Detection System

> Rule-based forensic image analysis · Java Spring Boot · Premium Apple-level UI

---

## Project Overview

VeraLens is a full-stack deepfake detection system that uses **deterministic, rule-based logic** — no machine learning required. Users upload an image, the Java backend applies four independent forensic algorithms, and the system returns a **REAL** or **FAKE** verdict with a confidence score.

---

## Project Structure

```
deepfake-detector/
│
├── frontend/
│   ├── index.html          # Full single-page application
│   ├── style.css           # Premium dark/light theme styling
│   └── script.js           # Upload, API integration, animations
│
└── backend/
    ├── pom.xml             # Maven build config (Spring Boot 3.2)
    └── src/main/java/com/veralens/detector/
        ├── DeepfakeDetectorApplication.java   # Spring Boot entry point
        ├── controller/
        │   └── AnalysisController.java        # POST /analyze endpoint
        ├── service/
        │   └── ImageAnalysisService.java      # 4-rule detection engine
        └── model/
            └── AnalysisResult.java            # Response DTO
```

---

## System Architecture

```
[User Browser]
     │
     │  POST /analyze (multipart/form-data)
     ▼
[Spring Boot Controller]  ←────────────────────────────┐
     │                                                  │
     ▼                                                 JSON
[ImageAnalysisService]                                Response
     │
     ├── Rule 1: Pixel Intensity Variance
     │     → Statistical variance across grayscale pixels
     │     → Flags if variance < 800 (too uniform) or > 18000 (too noisy)
     │
     ├── Rule 2: Blur / Smoothing Detection
     │     → Laplacian kernel applied to measure edge sharpness
     │     → Flags if Laplacian variance < 150 (over-smoothed)
     │
     ├── Rule 3: Edge Sharpness Inconsistency
     │     → Sobel gradient magnitude across 4×4 grid of regions
     │     → Coefficient of Variation > 0.55 → inconsistent
     │
     └── Rule 4: Facial Symmetry Analysis
           → Left/right pixel mean intensity comparison
           → Diff < 1.5 (hyper-symmetric) or > 22 (broken) → suspicious
```

**Scoring:**
- Each triggered rule adds its weight to the total score
- Weights: Variance(25) + Blur(30) + Edge(25) + Symmetry(20) = 100
- Score ≥ 45 → **FAKE**
- Score < 45 → **REAL**
- Confidence = 50 + (score-proportion × 50), clamped to 51–99%

---

## API Reference

### `POST /analyze`

**Request:**
```
Content-Type: multipart/form-data
Field: image (File) — JPEG, PNG, or WebP, max 10MB
```

**Response (200 OK):**
```json
{
  "result": "FAKE",
  "confidence": 82,
  "rules": [
    { "name": "Pixel Intensity Variance",   "triggered": true,  "score": 74 },
    { "name": "Blur / Smoothing Detection", "triggered": true,  "score": 88 },
    { "name": "Edge Sharpness Inconsistency","triggered": false, "score": 0  },
    { "name": "Facial Symmetry Analysis",   "triggered": true,  "score": 55 }
  ]
}
```

**Error responses:**
- `400` — No file, invalid content-type, or file > 10MB
- `500` — Could not read/decode image

---

## How to Run Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- Any modern web browser

### Step 1 — Start the Backend

```bash
cd deepfake-detector/backend

# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run
```

The backend starts at **http://localhost:8080**

To verify:
```bash
curl -X POST http://localhost:8080/analyze \
  -F "image=@/path/to/your/image.jpg"
```

### Step 2 — Open the Frontend

```bash
cd deepfake-detector/frontend

# Option A: Just open in browser
open index.html

# Option B: Serve with Python (recommended to avoid CORS issues with some browsers)
python3 -m http.server 3000
# Then visit http://localhost:3000
```

> **Note:** If the backend is not running, the frontend automatically enters **demo mode** and shows a simulated result so you can explore the UI.

### Step 3 — Use the App

1. Click **"Try Now"** or scroll to the detection section
2. Drag & drop or click to upload any image
3. Click **"Analyze Image"**
4. View the REAL/FAKE verdict with confidence score and rule breakdown

---

## Key Technical Decisions

### Why Rule-Based (No ML)?

- **Interpretability**: Every decision has an explicit, inspectable reason
- **No training data**: Works immediately with no dataset required
- **Deterministic**: Same image always produces the same result
- **Lightweight**: No GPU, no model weights, no inference libraries

### Java ImageIO + BufferedImage

The detection logic uses only Java's standard library:
- `BufferedImage.getRGB()` for pixel access
- Manual Laplacian and Sobel kernel convolutions
- ITU-R BT.601 luminosity formula for grayscale conversion

### Frontend Architecture

- **Zero frameworks**: Pure HTML/CSS/JS — fast, portable, no build step
- **Demo mode**: Works without backend (simulates API response)
- **Dark/Light theme**: CSS custom properties + `data-theme` attribute
- **Drag & drop + file picker**: Both input paths supported
- **Toast notifications**: Non-blocking user feedback system

---

## Detection Logic Detail

### Rule 1: Pixel Intensity Variance
```
variance = E[X²] - E[X]²  across sampled grayscale pixels
suspicious if: variance < 800  OR  variance > 18000
```
AI faces often have unnaturally smooth skin → low variance. Some GAN artifacts create noise bursts → high variance.

### Rule 2: Blur/Smoothing (Laplacian)
```
Laplacian = pixel[y-1][x] + pixel[y+1][x] + pixel[y][x-1] + pixel[y][x+1] - 4·pixel[y][x]
blurry if: variance(Laplacian values) < 150
```
A blurred image has low Laplacian response variance. AI generators often over-smooth facial boundaries.

### Rule 3: Edge Inconsistency (Sobel)
```
Sobel magnitude at (x,y) = sqrt(Gx² + Gy²)
  where Gx = pixel[y][x+1] - pixel[y][x-1]
        Gy = pixel[y+1][x] - pixel[y-1][x]
CV = stdDev(regionMagnitudes) / mean(regionMagnitudes)
suspicious if: CV > 0.55
```
Real images have consistent natural texture. Deepfakes often have one hyper-sharp region (face) adjacent to blurred or composited background.

### Rule 4: Symmetry Approximation
```
leftMean  = avg(pixels in left half)
rightMean = avg(mirror-sampled pixels in right half)
diff = |leftMean - rightMean|
suspicious if: diff < 1.5  OR  diff > 22
```
Genuine faces have moderate asymmetry (lighting, pose). GAN faces can be hyper-symmetric (direct face-forward) or have broken symmetry from failed compositing.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend Framework | Spring Boot 3.2 (Java 17) |
| Image Processing | Java AWT, ImageIO, BufferedImage |
| REST API | Spring MVC (@RestController) |
| Frontend | Vanilla HTML5 / CSS3 / ES6+ |
| Fonts | Syne (display) + DM Sans (body) |
| Build Tool | Maven |

---

## Extending the System

To add a new detection rule:

1. Add a `private RuleOutput checkYourRule(double[][] gray, int w, int h)` method in `ImageAnalysisService.java`
2. Define a threshold constant and a weight constant
3. Call it in the `analyze()` method and add the weight to `totalScore`
4. Update `maxPossible` if the weight sum changes

---

*Built as a portfolio-grade demonstration of rule-based computer vision in Java.*
