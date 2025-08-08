  window.addEventListener("hashchange", renderPage);
  window.addEventListener("load", renderPage);

  function renderPage() {
    const route = location.hash.slice(1) || "home";
    const app = document.getElementById("app");

    if (route === "home") {
      app.innerHTML = renderHomePage();
      bindHomePageEvents();
    } else if (route === "about") {
      app.innerHTML = renderAboutPage();
    } else {
      app.innerHTML = `<div class="container"><h2>404 - Page Not Found</h2></div>`;
    }
  }

  function renderHomePage() {
    return `
      <div class="container">
        <h2>Upload and Split PDF</h2>
          <div class="card">
        <label for="pdfFile">Upload PDF</label>
        <input type="file" id="pdfFile" accept="application/pdf" />
          </div>
          <div class="card">
        <label for="splitOption">Split Option</label>
        <select id="splitOption" onchange="toggleEqualInput()">
          <option value="blank">Split by Blank Pages</option>
          <option value="equal">Split by Equal Sizes</option>
          <option value="range">Split by Page Range</option>
        </select>

        <div id="equalSizeInput" class="hidden">
          <label for="pageCount">Pages per split</label>
          <input type="number" id="pageCount" min="1" placeholder="e.g., 5" />
        </div>

        <div id="customSplitInput" class="hidden">
    <label for="customPages">Page Ranges (e.g., 1-3,5-7)</label>
    <input type="text" id="customPages" placeholder="e.g., 1-3,5-7" />
  </div>


          </div>
        <button id="splitBtn">Split PDF</button>
        <div id="statusMessage" class="hidden status"></div>
      </div>
    `;
  }

  function bindHomePageEvents() {
    document.getElementById("splitOption").addEventListener("change", toggleEqualInput);
    document.getElementById("splitBtn").addEventListener("click", submitForm);
  }

  function toggleEqualInput() {
    const value = document.getElementById('splitOption').value;

    const equalInput = document.getElementById('equalSizeInput');
    const rangeInput = document.getElementById('customSplitInput');

    equalInput.classList.add('hidden');
    rangeInput.classList.add('hidden');

    if (value === 'equal') {
      equalInput.classList.remove('hidden');
    } else if (value === 'range') {
      rangeInput.classList.remove('hidden');
    }
  }



  function submitForm() {
    const file = document.getElementById("pdfFile").files[0];
    const option = document.getElementById("splitOption").value;
    const pageCount = document.getElementById("pageCount").value;
    const customPages = document.getElementById("customPages").value;

    if (!file) {
      alert("Please upload a PDF file.");
      return;
    }

    if (option === "equal" && (!pageCount || pageCount <= 0)) {
      alert("Enter a valid number of pages.");
      return;
    }

    if (option === "range" && !/^\d+-\d+(,\d+-\d+)*$/.test(customPages.trim())) {
      alert("Enter valid page ranges (e.g., 1-3,5-7)");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    const backendBaseURL = "http://localhost:8080";
    let splitURL = "";
  if (option === "blank") {
      splitURL = `${backendBaseURL}/split/blank`;
  } else if (option === "equal") {
      formData.append("chunkSize", pageCount);
      splitURL = `${backendBaseURL}/split/equal`;
  } else if (option === "range") {
      customPages.trim().split(",").forEach(r => formData.append("ranges", r.trim()));
      splitURL = `${backendBaseURL}/split/ranges`;
  }

    // Step 1: Call the split endpoint
    fetch(splitURL, {
    method: "POST",
    body: formData,
  })
.then(async response => {
  if (!response.ok) {
    const errorMessage = await response.text();
    throw new Error(errorMessage || "Split failed");
  }
  showStatus("Splitting successful. Download starting...", false);
  return response.blob();
})
.then(blob => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "split_pdfs.zip";
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
})
.catch(error => {
  console.error("Error:", error);
  showStatus(error.message || "Something went wrong during the process.", true);
});

  }
function showStatus(message, isError = false) {
  const statusDiv = document.getElementById("statusMessage");
  statusDiv.textContent = message;
  statusDiv.className = `status ${isError ? 'error' : 'success'}`;
  statusDiv.classList.remove("hidden");
}


  function renderAboutPage() {
    return `
      <div class="container">
        <h2>About This Tool</h2>
        <p>This is a lightweight PDF splitter that supports three methods:</p>
        <ul>
          <li><strong>Split by Blank Pages</strong>: Detects blank pages and splits the file accordingly.</li>
          <li><strong>Split by Equal Sizes</strong>: Splits the file into equal chunks based on number of pages.</li>
          <li><strong>Split by Custom Ranges</strong>: Splits the file into segments based on user input.</li>
        </ul>
      </div>
    `;
  }
