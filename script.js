/* ============================================
   VeraLens — script.js
   Deepfake Detection System Frontend Logic
   ============================================ */

const API_URL = 'http://localhost:8080/analyze';

// ===== STATE =====
let selectedFile = null;

// ===== THEME TOGGLE =====
const themeToggle = document.getElementById('themeToggle');
const html = document.documentElement;

function initTheme() {
  const saved = localStorage.getItem('veralens-theme') || 'dark';
  html.setAttribute('data-theme', saved);
}

themeToggle.addEventListener('click', () => {
  const current = html.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  localStorage.setItem('veralens-theme', next);
});

initTheme();

// ===== NAVBAR SCROLL =====
const navbar = document.getElementById('navbar');
window.addEventListener('scroll', () => {
  navbar.classList.toggle('scrolled', window.scrollY > 20);
}, { passive: true });

// ===== HAMBURGER MENU =====
const hamburger = document.getElementById('hamburger');
const mobileMenu = document.getElementById('mobileMenu');

hamburger.addEventListener('click', () => {
  hamburger.classList.toggle('open');
  mobileMenu.classList.toggle('open');
});

function closeMobileMenu() {
  hamburger.classList.remove('open');
  mobileMenu.classList.remove('open');
}

// ===== REVEAL ON SCROLL =====
const revealEls = document.querySelectorAll('.reveal');
const revealObserver = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
    }
  });
}, { threshold: 0.12 });

revealEls.forEach(el => revealObserver.observe(el));

// ===== FILE UPLOAD =====
const uploadZone = document.getElementById('uploadZone');
const fileInput = document.getElementById('fileInput');
const uploadIdle = document.getElementById('uploadIdle');
const uploadPreview = document.getElementById('uploadPreview');
const previewImg = document.getElementById('previewImg');
const analyzeBtn = document.getElementById('analyzeBtn');

uploadZone.addEventListener('click', (e) => {
  if (e.target.closest('.preview-overlay')) return;
  if (uploadPreview.style.display === 'block') return;
  fileInput.click();
});

fileInput.addEventListener('change', (e) => {
  const file = e.target.files[0];
  if (file) handleFileSelected(file);
});

// Drag and drop
uploadZone.addEventListener('dragover', (e) => {
  e.preventDefault();
  uploadZone.classList.add('dragging');
});
uploadZone.addEventListener('dragleave', () => {
  uploadZone.classList.remove('dragging');
});
uploadZone.addEventListener('drop', (e) => {
  e.preventDefault();
  uploadZone.classList.remove('dragging');
  const file = e.dataTransfer.files[0];
  if (file) handleFileSelected(file);
});

function handleFileSelected(file) {
  if (!file.type.startsWith('image/')) {
    showToast('Please upload a valid image file (JPEG, PNG, WebP).', 'error');
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('File is too large. Maximum size is 10MB.', 'error');
    return;
  }
  selectedFile = file;
  const reader = new FileReader();
  reader.onload = (e) => {
    previewImg.src = e.target.result;
    uploadIdle.style.display = 'none';
    uploadPreview.style.display = 'block';
    analyzeBtn.disabled = false;
    // Reset result if re-uploading
    document.getElementById('resultArea').style.display = 'none';
  };
  reader.readAsDataURL(file);
  showToast(`Image loaded: ${file.name}`, 'success');
}

function resetUpload() {
  selectedFile = null;
  fileInput.value = '';
  previewImg.src = '';
  uploadIdle.style.display = 'block';
  uploadPreview.style.display = 'none';
  analyzeBtn.disabled = true;
  document.getElementById('resultArea').style.display = 'none';
}

// ===== ANALYSIS =====
async function analyzeImage() {
  if (!selectedFile) {
    showToast('Please upload an image first.', 'error');
    return;
  }

  const btnText = document.querySelector('.btn-text');
  const btnSpinner = document.getElementById('btnSpinner');
  
  // Loading state
  analyzeBtn.disabled = true;
  btnText.style.display = 'none';
  btnSpinner.style.display = 'flex';
  showToast('Analyzing image...', 'info');

  const formData = new FormData();
  formData.append('image', selectedFile);

  try {
    const response = await fetch(API_URL, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `Server error: ${response.status}`);
    }

    const data = await response.json();
    displayResult(data);
    showToast(`Analysis complete: ${data.result}`, data.result === 'REAL' ? 'success' : 'error');

  } catch (err) {
    // Demo mode: if backend is not running, show simulated result
    if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError') || err.message.includes('Load failed')) {
      showToast('Backend not connected — showing demo result.', 'info');
      const demo = generateDemoResult();
      displayResult(demo);
    } else {
      showToast(`Error: ${err.message}`, 'error');
      console.error('Analysis error:', err);
    }
  } finally {
    btnText.style.display = 'block';
    btnSpinner.style.display = 'none';
    analyzeBtn.disabled = false;
  }
}

// Demo result when backend not running
function generateDemoResult() {
  const isFake = Math.random() > 0.5;
  const confidence = isFake
    ? Math.floor(Math.random() * 30) + 65
    : Math.floor(Math.random() * 20) + 78;
  return {
    result: isFake ? 'FAKE' : 'REAL',
    confidence: confidence,
    rules: [
      { name: 'Pixel Intensity Variance', triggered: isFake ? Math.random() > 0.4 : Math.random() > 0.8, score: Math.floor(Math.random() * 40) + 30 },
      { name: 'Blur / Smoothing Detection', triggered: isFake ? Math.random() > 0.3 : Math.random() > 0.9, score: Math.floor(Math.random() * 40) + 20 },
      { name: 'Edge Sharpness Inconsistency', triggered: isFake ? Math.random() > 0.35 : Math.random() > 0.85, score: Math.floor(Math.random() * 50) + 25 },
      { name: 'Facial Symmetry Analysis', triggered: isFake ? Math.random() > 0.45 : Math.random() > 0.9, score: Math.floor(Math.random() * 35) + 15 },
    ]
  };
}

function displayResult(data) {
  const resultArea = document.getElementById('resultArea');
  const resultBadge = document.getElementById('resultBadge');
  const resultIcon = document.getElementById('resultIcon');
  const resultLabel = document.getElementById('resultLabel');
  const resultDesc = document.getElementById('resultDesc');
  const confidenceFill = document.getElementById('confidenceFill');
  const confidenceNum = document.getElementById('confidenceNum');
  const ruleBreakdown = document.getElementById('ruleBreakdown');

  const isReal = data.result === 'REAL';
  const confidence = Math.min(100, Math.max(0, data.confidence || 0));

  // Show area
  resultArea.style.display = 'block';

  // Badge
  resultBadge.className = `result-badge ${isReal ? 'real' : 'fake'}`;
  resultIcon.className = `result-icon ${isReal ? 'real' : 'fake'}`;
  resultIcon.textContent = isReal ? '✓' : '✕';
  resultLabel.textContent = data.result;

  // Description
  resultDesc.textContent = isReal
    ? 'No significant forensic irregularities detected. Pixel variance, edge sharpness, and symmetry metrics are within expected ranges for a natural photograph.'
    : 'Forensic analysis detected anomalies consistent with synthetic image generation. One or more rule-based checks exceeded the threshold for authenticity.';

  // Confidence bar
  confidenceFill.className = `confidence-fill ${isReal ? 'real' : 'fake'}`;
  confidenceNum.textContent = '0%';
  setTimeout(() => {
    confidenceFill.style.width = `${confidence}%`;
    animateNumber(confidenceNum, 0, confidence, 1200, (v) => `${v}%`);
  }, 100);

  // Rule breakdown
  ruleBreakdown.innerHTML = '';
  if (data.rules && Array.isArray(data.rules)) {
    data.rules.forEach((rule, i) => {
      const item = document.createElement('div');
      item.className = 'rule-item';
      item.style.animationDelay = `${i * 80}ms`;
      item.innerHTML = `
        <div class="rule-item-left">
          <div class="rule-dot ${rule.triggered ? 'triggered' : 'ok'}"></div>
          <span>${rule.name}</span>
        </div>
        <span class="rule-score">${rule.triggered ? '⚠ Anomaly' : '✓ Normal'} · ${rule.score || 0}%</span>
      `;
      ruleBreakdown.appendChild(item);
    });
  }

  // Scroll into view
  resultArea.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// ===== UTILITY: Animate number =====
function animateNumber(el, from, to, duration, format) {
  const start = performance.now();
  function step(now) {
    const progress = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 4);
    el.textContent = format(Math.round(from + (to - from) * eased));
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

// ===== TOAST =====
function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;

  const icons = { success: '✓', error: '✕', info: 'ℹ' };
  toast.innerHTML = `
    <span class="toast-icon">${icons[type] || 'ℹ'}</span>
    <span>${message}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = 'toastOut 0.3s ease forwards';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// ===== SMOOTH SCROLL =====
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
  anchor.addEventListener('click', (e) => {
    const target = document.querySelector(anchor.getAttribute('href'));
    if (target) {
      e.preventDefault();
      const offset = 80;
      const top = target.getBoundingClientRect().top + window.scrollY - offset;
      window.scrollTo({ top, behavior: 'smooth' });
    }
  });
});
