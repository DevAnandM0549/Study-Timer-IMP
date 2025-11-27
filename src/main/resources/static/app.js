// Global State
let currentUser = null;
let authCredentials = null;
let currentTimer = null;
let timerInterval = null;
let currentSession = 'work';
let currentTask = null;
let taskFiles = {};
let previousProgress = null;

const sessionDurations = {
    work: 25 * 60,
    shortBreak: 5 * 60,
    longBreak: 15 * 60
};

const LEVEL_THRESHOLDS = [0, 500, 1500, 3000, 5000];
const LEVEL_TITLES = ['Beginner Scholar', 'Dedicated Learner', 'Knowledge Seeker', 'Study Master', 'Legendary Scholar'];

// ==================== PAGE NAVIGATION ====================
function showPage(pageId) {
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => section.classList.remove('active'));
    document.getElementById(sectionId).classList.add('active');
}

function showDashboard() {
    showPage('dashboardPage');
    showSection('homeSection');
    loadDashboardData();
}

// Navigation helpers
function goToTimer() {
    showSection('timerSection');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('navTimer').classList.add('active');
}

function goToTasks() {
    showSection('tasksSection');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('navTasks').classList.add('active');
    loadTasks();
}

// ==================== NOTIFICATIONS ====================
function showNotification(icon, title, message) {
    document.getElementById('notificationIcon').textContent = icon;
    document.getElementById('notificationTitle').textContent = title;
    document.getElementById('notificationMessage').textContent = message;
    document.getElementById('notificationPopup').classList.remove('hidden');
}

function closeNotification() {
    document.getElementById('notificationPopup').classList.add('hidden');
}

function checkForNewAchievements(oldAchievements, newAchievements) {
    if (!oldAchievements) return;
    
    newAchievements.forEach(newAch => {
        const oldAch = oldAchievements.find(a => a.achievementKey === newAch.achievementKey);
        if (oldAch && !oldAch.isUnlocked && newAch.isUnlocked) {
            showNotification('🏆', 'Achievement Unlocked!', `${newAch.name} - +${newAch.xpReward} XP`);
        }
    });
}

function checkForLevelUp(oldLevel, newLevel) {
    if (oldLevel && newLevel > oldLevel) {
        showNotification('⬆️', 'Level Up!', `Congratulations! You reached Level ${newLevel}: ${LEVEL_TITLES[newLevel - 1]}`);
    }
}

// ==================== AUTH ====================
function showAuthTab(tabName) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`${tabName}Content`).classList.add('active');
}

// Sign Up
document.getElementById('signUpForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('signUpUsername').value;
    const email = document.getElementById('signUpEmail').value;
    const password = document.getElementById('signUpPassword').value;
    const confirmPassword = document.getElementById('signUpConfirmPassword').value;

    if (password !== confirmPassword) {
        showError('signUpError', 'Passwords do not match');
        return;
    }

    try {
        const response = await fetch('/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });

        if (response.ok) {
            showSuccess('signUpSuccess', 'Registration successful! Please sign in.');
            document.getElementById('signUpForm').reset();
            setTimeout(() => showAuthTab('signIn'), 2000);
        } else {
            const errorData = await response.json();
            showError('signUpError', errorData.error || 'Registration failed');
        }
    } catch (error) {
        showError('signUpError', 'Registration failed. Please try again.');
    }
});

// Sign In
document.getElementById('signInForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('signInUsername').value;
    const password = document.getElementById('signInPassword').value;

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            currentUser = await response.json();
            authCredentials = btoa(username + ':' + password);
            console.log('Login successful:', currentUser);
            showDashboard();
        } else {
            showError('signInError', 'Invalid username or password');
        }
    } catch (error) {
        showError('signInError', 'Login failed. Please try again.');
    }
});

function logout() {
    currentUser = null;
    authCredentials = null;
    previousProgress = null;
    clearTimer();
    showPage('authPage');
    document.getElementById('signInForm').reset();
}

// ==================== HELPER FUNCTIONS ====================
function showError(elementId, message) {
    const errorEl = document.getElementById(elementId);
    errorEl.textContent = message;
    errorEl.classList.add('show');
    setTimeout(() => errorEl.classList.remove('show'), 5000);
}

function showSuccess(elementId, message) {
    const successEl = document.getElementById(elementId);
    successEl.textContent = message;
    successEl.classList.add('show');
    setTimeout(() => successEl.classList.remove('show'), 5000);
}

// ==================== AUTHENTICATED API CALLS ====================
async function authenticatedFetch(url, options = {}) {
    if (!authCredentials) {
        console.error('No authentication credentials available');
        return null;
    }

    const headers = { ...options.headers, 'Authorization': 'Basic ' + authCredentials };
    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
        alert('Session expired. Please login again.');
        logout();
        return null;
    }
    return response;
}


// ==================== DASHBOARD ====================
async function loadDashboardData() {
    if (!currentUser) return;
    document.getElementById('welcomeUser').textContent = currentUser.username;
    await loadStats();
    await loadTasks();
}

async function loadStats() {
    try {
        const response = await authenticatedFetch('/api/progress');
        if (response && response.ok) {
            const progress = await response.json();
            
            // Check for level up
            if (previousProgress) {
                checkForLevelUp(previousProgress.currentLevel, progress.currentLevel);
            }
            
            document.getElementById('totalSessions').textContent = progress.totalSessions || 0;
            document.getElementById('totalMinutes').textContent = progress.totalMinutes || 0;
            document.getElementById('currentStreak').textContent = progress.currentStreak || 0;
            document.getElementById('totalXP').textContent = progress.totalXp || 0;
            
            // Update level progress
            updateLevelProgress(progress);
            previousProgress = progress;
        }
    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

function updateLevelProgress(progress) {
    const level = progress.currentLevel || 1;
    const totalXp = progress.totalXp || 0;
    
    document.getElementById('currentLevel').textContent = level;
    document.getElementById('levelTitle').textContent = LEVEL_TITLES[level - 1] || 'Scholar';
    
    const currentThreshold = LEVEL_THRESHOLDS[level - 1] || 0;
    const nextThreshold = LEVEL_THRESHOLDS[level] || LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1];
    const xpInLevel = totalXp - currentThreshold;
    const xpNeeded = nextThreshold - currentThreshold;
    const progressPercent = Math.min((xpInLevel / xpNeeded) * 100, 100);
    
    document.getElementById('xpProgressFill').style.width = progressPercent + '%';
    document.getElementById('currentXpLabel').textContent = totalXp + ' XP';
    document.getElementById('nextLevelXpLabel').textContent = nextThreshold + ' XP';
    document.getElementById('xpToNext').textContent = level >= 5 ? 'Max Level!' : `${nextThreshold - totalXp} XP to next level`;
}

// ==================== TIMER SECTION ====================
function selectSession(type) {
    currentSession = type;
    document.querySelectorAll('.session-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    resetTimer();
}

function resetTimer() {
    clearInterval(timerInterval);
    currentTimer = sessionDurations[currentSession];
    updateTimerDisplay();
    document.getElementById('startBtn').disabled = false;
    document.getElementById('pauseBtn').disabled = true;
    document.getElementById('resetBtn').disabled = true;
}

function startTimer() {
    if (currentTimer === null) currentTimer = sessionDurations[currentSession];
    document.getElementById('startBtn').disabled = true;
    document.getElementById('pauseBtn').disabled = false;
    document.getElementById('resetBtn').disabled = false;
    document.querySelector('.timer-circle').classList.add('running');

    timerInterval = setInterval(() => {
        currentTimer--;
        updateTimerDisplay();
        if (currentTimer <= 0) completeSession();
    }, 1000);
}

function pauseTimer() {
    clearInterval(timerInterval);
    document.getElementById('startBtn').disabled = false;
    document.getElementById('pauseBtn').disabled = true;
    document.querySelector('.timer-circle').classList.remove('running');
}

function clearTimer() {
    clearInterval(timerInterval);
    currentTimer = null;
    currentTask = null;
    document.getElementById('currentTaskDisplay').classList.add('hidden');
    document.getElementById('documentViewerPanel').classList.add('hidden');
    document.querySelector('.timer-circle').classList.remove('running');
    resetTimer();
}

function updateTimerDisplay() {
    const minutes = Math.floor(currentTimer / 60);
    const seconds = currentTimer % 60;
    document.getElementById('timerDisplay').textContent = 
        `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

async function completeSession() {
    clearInterval(timerInterval);
    
    // Store old progress and achievements for comparison
    let oldProgress = previousProgress ? { ...previousProgress } : null;
    let oldAchievements = null;
    try {
        const achResponse = await authenticatedFetch('/api/progress/achievements');
        if (achResponse && achResponse.ok) {
            oldAchievements = await achResponse.json();
        }
    } catch (e) {}

    // Save the session to backend with actual minutes
    try {
        const sessionTypeMap = { 'work': 'WORK', 'shortBreak': 'SHORT_BREAK', 'longBreak': 'LONG_BREAK' };
        const actualMinutes = currentTask ? currentTask.minutes : sessionDurations[currentSession] / 60;
        const sessionData = {
            sessionType: sessionTypeMap[currentSession],
            taskId: currentTask ? currentTask.id : null,
            minutes: actualMinutes
        };
        const response = await authenticatedFetch('/api/progress/session', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sessionData)
        });

        if (response && response.ok) {
            console.log('Session saved successfully');
        }
    } catch (error) {
        console.error('Failed to save session:', error);
    }

    // If this was a work session with a task, auto-complete the task
    const taskToComplete = currentTask;
    if (currentSession === 'work' && taskToComplete) {
        try {
            const response = await authenticatedFetch(`/api/tasks/${taskToComplete.id}/complete`, { method: 'PUT' });
            if (response && response.ok) {
                showNotification('🎉', 'Task Completed!', `Great job! You finished "${taskToComplete.title}"!`);
            }
        } catch (error) {
            console.error('Failed to auto-complete task:', error);
        }
    }

    // Show session complete message
    const completeEl = document.getElementById('sessionComplete');
    completeEl.classList.remove('hidden');
    setTimeout(() => completeEl.classList.add('hidden'), 5000);

    // Clear current task
    currentTask = null;
    document.getElementById('currentTaskDisplay').classList.add('hidden');
    document.getElementById('documentViewerPanel').classList.add('hidden');

    // Load updated stats
    await loadStats();
    await loadTasks();
    
    // Check for XP gain and show popup
    if (oldProgress && previousProgress) {
        const xpGained = previousProgress.totalXp - oldProgress.totalXp;
        if (xpGained > 0) {
            setTimeout(() => {
                showNotification('⭐', 'XP Earned!', `+${xpGained} XP gained!`);
            }, 500);
        }
    }
    
    // Check for level up
    if (oldProgress && previousProgress && previousProgress.currentLevel > oldProgress.currentLevel) {
        setTimeout(() => {
            showNotification('⬆️', 'Level Up!', `Congratulations! You reached Level ${previousProgress.currentLevel}!`);
        }, 1500);
    }
    
    // Check for new achievements
    try {
        const newAchResponse = await authenticatedFetch('/api/progress/achievements');
        if (newAchResponse && newAchResponse.ok) {
            const newAchievements = await newAchResponse.json();
            checkForNewAchievements(oldAchievements, newAchievements);
        }
    } catch (e) {}

    resetTimer();
}


// ==================== TASKS SECTION ====================
async function loadTasks() {
    try {
        const response = await authenticatedFetch(`/api/tasks/user/${currentUser.id}`);
        if (response && response.ok) {
            const tasks = await response.json();
            displayTasks(tasks);
        }
    } catch (error) {
        console.error('Failed to load tasks:', error);
    }
}

function displayTasks(tasks) {
    const activeTasks = tasks.filter(t => !t.isCompleted);
    const completedTasks = tasks.filter(t => t.isCompleted);
    displayTaskList('activeTasksList', activeTasks, false);
    displayTaskList('completedTasksList', completedTasks, true);
}

function displayTaskList(containerId, tasks, isCompleted) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';

    if (tasks.length === 0) {
        const emptyMsg = isCompleted 
            ? 'Complete tasks to see them here! 💪' 
            : 'No active tasks. Create one above! 🚀';
        container.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 40px;">${emptyMsg}</p>`;
        return;
    }

    tasks.forEach(task => {
        const taskEl = document.createElement('div');
        taskEl.className = 'task-item';
        taskEl.onclick = () => openTaskModal(task);
        // Use estimatedMinutes if available, otherwise fall back to pomodoros * 25
        const taskMinutes = task.estimatedMinutes || (task.estimatedPomodoros * 25);
        // Store task in a global map so we can access it by ID
        window.taskCache = window.taskCache || {};
        window.taskCache[task.id] = task;
        taskEl.innerHTML = `
            <div class="task-info">
                <div class="task-title">${task.title}</div>
                <div class="task-meta">
                    <span>⏱️ ${taskMinutes} min</span>
                    <span class="priority-badge priority-${task.priority}">${task.priority}</span>
                    <span>📊 ${task.completedPomodoros}/${task.estimatedPomodoros} pomodoros</span>
                </div>
            </div>
            <div class="task-actions" onclick="event.stopPropagation()">
                ${!isCompleted ? `
                    <button class="task-action-btn" onclick="event.stopPropagation(); startTaskById(${task.id})">⏱️ Start</button>
                    <button class="task-action-btn" onclick="event.stopPropagation(); completeTask(${task.id}, '${task.title.replace(/'/g, "\\'")}')">✓ Complete</button>
                ` : `
                    <button class="task-action-btn" onclick="event.stopPropagation(); uncompleteTask(${task.id})">↩️ Restore</button>
                `}
                <button class="task-action-btn" onclick="event.stopPropagation(); deleteTask(${task.id})">🗑️</button>
            </div>
        `;
        container.appendChild(taskEl);
    });
}

async function startTaskById(taskId) {
    const task = window.taskCache ? window.taskCache[taskId] : null;
    if (task) {
        const taskMinutes = task.estimatedMinutes || (task.estimatedPomodoros * 25);
        await startTaskFromList(taskId, task.title, taskMinutes);
    }
}

async function startTaskFromList(taskId, taskTitle, taskMinutes = 25) {
    currentTask = { id: taskId, title: taskTitle, minutes: taskMinutes };
    document.getElementById('currentTaskName').textContent = taskTitle;
    document.getElementById('currentTaskDisplay').classList.remove('hidden');
    showSection('timerSection');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('navTimer').classList.add('active');
    
    // Set timer to task's duration in seconds
    currentSession = 'work';
    document.querySelectorAll('.session-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector('.session-btn').classList.add('active');
    currentTimer = taskMinutes * 60;
    updateTimerDisplay();
    document.getElementById('startBtn').disabled = false;
    
    // Load files from server for this task
    await loadTaskFiles(taskId);
    document.getElementById('pauseBtn').disabled = true;
    document.getElementById('resetBtn').disabled = true;
    
    // Load study materials into document viewer
    loadTaskFilesIntoViewer(taskId);
}

// ==================== DOCUMENT VIEWER ====================
function loadTaskFilesIntoViewer(taskId) {
    const files = taskFiles[taskId] || [];
    const selector = document.getElementById('fileSelector');
    selector.innerHTML = '<option value="">Select a file...</option>';
    
    if (files.length > 0) {
        files.forEach((file, index) => {
            const option = document.createElement('option');
            option.value = index;
            option.textContent = file.name;
            selector.appendChild(option);
        });
        
        // Auto-select first file and show viewer
        selector.value = '0';
        displayFileInViewer(files[0]);
        document.getElementById('documentViewerPanel').classList.remove('hidden');
    } else {
        document.getElementById('documentViewerPanel').classList.add('hidden');
    }
}

function switchFile() {
    const selector = document.getElementById('fileSelector');
    const index = selector.value;
    
    if (index === '' || !currentTask) {
        document.getElementById('documentViewerContent').innerHTML = `
            <div class="no-file-message">
                <p>📁</p>
                <p>No study material loaded</p>
            </div>
        `;
        return;
    }
    
    const files = taskFiles[currentTask.id] || [];
    if (files[index]) {
        displayFileInViewer(files[index]);
    }
}

function displayFileInViewer(file) {
    const container = document.getElementById('documentViewerContent');
    
    if (file.type === 'application/pdf' || file.name.endsWith('.pdf')) {
        // PDF - use object tag with toolbar
        container.innerHTML = `
            <div class="pdf-viewer-wrapper">
                <div class="viewer-toolbar">
                    <button class="viewer-tool-btn" onclick="openCurrentFileInNewTab()">🔗 Open in New Tab</button>
                    <a href="${file.data}" download="${file.name}" class="viewer-tool-btn">⬇️ Download</a>
                </div>
                <object data="${file.data}" type="application/pdf" class="pdf-embed-viewer">
                    <div class="pdf-fallback">
                        <p>📄 PDF Preview</p>
                        <p>Your browser may not support inline PDF viewing</p>
                        <button class="btn-primary" onclick="openCurrentFileInNewTab()">Open in New Tab</button>
                    </div>
                </object>
            </div>
        `;
    } else if (file.type.startsWith('image/')) {
        // Image with zoom
        container.innerHTML = `
            <div class="image-viewer-container">
                <div class="viewer-toolbar">
                    <button class="viewer-tool-btn" onclick="zoomViewerImage(-0.2)">➖</button>
                    <button class="viewer-tool-btn" onclick="zoomViewerImage(0.2)">➕</button>
                    <button class="viewer-tool-btn" onclick="resetViewerZoom()">🔄</button>
                    <button class="viewer-tool-btn" onclick="openCurrentFileInNewTab()">🔗 New Tab</button>
                </div>
                <div class="image-scroll-wrapper">
                    <img id="viewerZoomImage" src="${file.data}" alt="${file.name}" style="transform: scale(1);">
                </div>
            </div>
        `;
    } else if (file.type === 'text/plain' || file.name.endsWith('.txt')) {
        // Text file
        container.innerHTML = `
            <div class="text-content-viewer">
                <div class="viewer-toolbar">
                    <button class="viewer-tool-btn" onclick="copyViewerText()">📋 Copy</button>
                    <button class="viewer-tool-btn" onclick="openCurrentFileInNewTab()">🔗 New Tab</button>
                </div>
                <pre id="viewerTextContent">${escapeHtml(file.textContent || file.content)}</pre>
            </div>
        `;
    } else {
        // Other files
        container.innerHTML = `
            <div class="no-file-message">
                <p>📄</p>
                <p>${file.name}</p>
                <p>This file type cannot be previewed directly</p>
                <a href="${file.data}" download="${file.name}" class="btn-primary" style="margin-top: 20px; text-decoration: none;">Download File</a>
            </div>
        `;
    }
    
    // Store current file reference for toolbar actions
    window.currentViewerFile = file;
}

let viewerZoom = 1;

function zoomViewerImage(delta) {
    viewerZoom = Math.max(0.5, Math.min(3, viewerZoom + delta));
    const img = document.getElementById('viewerZoomImage');
    if (img) img.style.transform = `scale(${viewerZoom})`;
}

function resetViewerZoom() {
    viewerZoom = 1;
    const img = document.getElementById('viewerZoomImage');
    if (img) img.style.transform = 'scale(1)';
}

function copyViewerText() {
    const textEl = document.getElementById('viewerTextContent');
    if (textEl) {
        navigator.clipboard.writeText(textEl.textContent).then(() => {
            showNotification('📋', 'Copied!', 'Text copied to clipboard');
        });
    }
}

function openCurrentFileInNewTab() {
    const file = window.currentViewerFile;
    if (!file || !file.data) return;
    
    const newWindow = window.open('', '_blank');
    if (file.type === 'application/pdf' || file.name.endsWith('.pdf')) {
        newWindow.document.write(`
            <!DOCTYPE html>
            <html>
            <head><title>${file.name} - StudyFlow</title></head>
            <body style="margin:0;padding:0;height:100vh;">
                <embed src="${file.data}" type="application/pdf" style="width:100%;height:100%;">
            </body>
            </html>
        `);
    } else if (file.type.startsWith('image/')) {
        newWindow.document.write(`
            <!DOCTYPE html>
            <html>
            <head><title>${file.name} - StudyFlow</title>
            <style>body{margin:0;background:#1a1f2e;display:flex;align-items:center;justify-content:center;min-height:100vh;}img{max-width:100%;max-height:100vh;}</style>
            </head>
            <body><img src="${file.data}" alt="${file.name}"></body>
            </html>
        `);
    } else if (file.type === 'text/plain' || file.name.endsWith('.txt')) {
        newWindow.document.write(`
            <!DOCTYPE html>
            <html>
            <head><title>${file.name} - StudyFlow</title>
            <style>body{margin:0;padding:20px;background:#1a1f2e;color:#f0f6fc;font-family:monospace;}pre{white-space:pre-wrap;word-wrap:break-word;}</style>
            </head>
            <body><pre>${escapeHtml(file.textContent || file.content)}</pre></body>
            </html>
        `);
    }
}

function toggleDocumentViewer() {
    const panel = document.getElementById('documentViewerPanel');
    panel.classList.toggle('hidden');
}

// ==================== TASK MODAL ====================
let currentModalTask = null;

async function openTaskModal(task) {
    currentModalTask = task;
    document.getElementById('modalTaskTitle').textContent = task.title;
    document.getElementById('modalTaskDescription').textContent = task.description || 'No description';
    document.getElementById('modalTaskPriority').textContent = task.priority;
    document.getElementById('modalTaskPriority').className = `priority-badge priority-${task.priority}`;
    const taskMinutes = task.estimatedMinutes || (task.estimatedPomodoros * 25);
    document.getElementById('modalTaskTime').textContent = `${taskMinutes} minutes`;
    
    // Load files from server
    await loadTaskFiles(task.id);
    
    // Display uploaded files for this task
    displayUploadedFiles(task.id);
    
    document.getElementById('taskModal').classList.remove('hidden');
}

function closeTaskModal() {
    document.getElementById('taskModal').classList.add('hidden');
    document.getElementById('studyViewer').classList.add('hidden');
    currentModalTask = null;
}

function startTaskTimer() {
    if (currentModalTask) {
        const taskMinutes = currentModalTask.estimatedMinutes || (currentModalTask.estimatedPomodoros * 25);
        startTaskFromList(currentModalTask.id, currentModalTask.title, taskMinutes);
        closeTaskModal();
    }
}

// ==================== FILE UPLOAD ====================

// Load files from server for a task
async function loadTaskFiles(taskId) {
    try {
        const response = await authenticatedFetch(`/api/tasks/${taskId}/files`);
        if (response && response.ok) {
            const files = await response.json();
            taskFiles[taskId] = files.map(f => ({
                id: f.id,
                name: f.name,
                type: f.type,
                data: f.data,
                textContent: f.textContent,
                content: f.type.startsWith('image/') ? `<img src="${f.data}" alt="${f.name}">` : 
                         (f.type === 'text/plain' ? `<pre>${f.textContent}</pre>` : `<p>📄 ${f.name}</p>`)
            }));
        }
    } catch (error) {
        console.error('Failed to load task files:', error);
    }
}

// Save file to server
async function saveFileToServer(taskId, fileData) {
    try {
        const response = await authenticatedFetch(`/api/tasks/${taskId}/files`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(fileData)
        });
        if (response && response.ok) {
            const saved = await response.json();
            return saved.id;
        }
    } catch (error) {
        console.error('Failed to save file:', error);
    }
    return null;
}

// Delete file from server
async function deleteFileFromServer(taskId, fileId) {
    try {
        await authenticatedFetch(`/api/tasks/${taskId}/files/${fileId}`, {
            method: 'DELETE'
        });
    } catch (error) {
        console.error('Failed to delete file:', error);
    }
}

function handleFileUpload(event) {
    const file = event.target.files[0];
    if (!file || !currentModalTask) return;

    const taskId = currentModalTask.id;
    if (!taskFiles[taskId]) taskFiles[taskId] = [];
    
    const fileType = file.type;
    
    // For PDFs and images, read as data URL
    if (fileType === 'application/pdf' || fileType.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = async function(e) {
            const fileData = {
                name: file.name,
                type: fileType,
                data: e.target.result,
                textContent: null,
                content: fileType.startsWith('image/') ? `<img src="${e.target.result}" alt="${file.name}">` : ''
            };
            // Save to server
            const fileId = await saveFileToServer(taskId, fileData);
            if (fileId) {
                fileData.id = fileId;
                taskFiles[taskId].push(fileData);
                displayUploadedFiles(taskId);
            }
        };
        reader.readAsDataURL(file);
    } 
    // For text files, read as text
    else if (fileType === 'text/plain' || file.name.endsWith('.txt')) {
        const reader = new FileReader();
        reader.onload = async function(e) {
            const fileData = {
                name: file.name,
                type: fileType || 'text/plain',
                textContent: e.target.result,
                data: null,
                content: `<pre>${e.target.result}</pre>`
            };
            // Save to server
            const fileId = await saveFileToServer(taskId, fileData);
            if (fileId) {
                fileData.id = fileId;
                taskFiles[taskId].push(fileData);
                displayUploadedFiles(taskId);
            }
        };
        reader.readAsText(file);
    }
    // For other files
    else {
        const reader = new FileReader();
        reader.onload = async function(e) {
            const fileData = {
                name: file.name,
                type: fileType,
                data: e.target.result,
                textContent: null,
                content: `<p>📄 ${file.name}</p>`
            };
            // Save to server
            const fileId = await saveFileToServer(taskId, fileData);
            if (fileId) {
                fileData.id = fileId;
                taskFiles[taskId].push(fileData);
                displayUploadedFiles(taskId);
            }
        };
        reader.readAsDataURL(file);
    }
    
    // Reset input
    event.target.value = '';
}

function displayUploadedFiles(taskId) {
    const container = document.getElementById('uploadedFiles');
    container.innerHTML = '';
    
    const files = taskFiles[taskId] || [];
    files.forEach((file, index) => {
        const fileEl = document.createElement('div');
        fileEl.className = 'uploaded-file';
        const fileId = file.id || 0;
        fileEl.innerHTML = `
            <span>📄 ${file.name}</span>
            <div class="file-actions">
                <button class="file-btn" onclick="viewFile(${taskId}, ${index})">👁️ View</button>
                <button class="file-btn" onclick="removeFile(${taskId}, ${index}, ${fileId})">🗑️</button>
            </div>
        `;
        container.appendChild(fileEl);
    });
}

function viewFile(taskId, fileIndex) {
    const file = taskFiles[taskId][fileIndex];
    document.getElementById('viewerFileName').textContent = file.name;
    
    const container = document.getElementById('viewerContent');
    
    if (file.type === 'application/pdf' || file.name.endsWith('.pdf')) {
        // PDF - use object tag with fallback options
        container.innerHTML = `
            <div class="pdf-modal-viewer">
                <div class="pdf-toolbar">
                    <button class="pdf-tool-btn" onclick="openFileInNewWindow(${taskId}, ${fileIndex})">🔗 Open in New Tab</button>
                    <a href="${file.data}" download="${file.name}" class="pdf-tool-btn">⬇️ Download</a>
                    <button class="pdf-tool-btn" onclick="toggleFullscreenViewer()">⛶ Fullscreen</button>
                </div>
                <object data="${file.data}" type="application/pdf" class="pdf-object-viewer">
                    <div class="pdf-fallback-message">
                        <p>📄 PDF cannot be displayed in browser</p>
                        <button class="btn-primary" onclick="openFileInNewWindow(${taskId}, ${fileIndex})">Open PDF in New Tab</button>
                    </div>
                </object>
            </div>
        `;
    } else if (file.type.startsWith('image/')) {
        // Image with zoom controls
        container.innerHTML = `
            <div class="image-modal-viewer">
                <div class="image-toolbar">
                    <button class="pdf-tool-btn" onclick="zoomImage(-0.2)">➖ Zoom Out</button>
                    <button class="pdf-tool-btn" onclick="zoomImage(0.2)">➕ Zoom In</button>
                    <button class="pdf-tool-btn" onclick="resetImageZoom()">🔄 Reset</button>
                    <button class="pdf-tool-btn" onclick="openFileInNewWindow(${taskId}, ${fileIndex})">🔗 Open in New Tab</button>
                </div>
                <div class="image-scroll-container">
                    <img id="zoomableImage" src="${file.data}" alt="${file.name}" style="transform: scale(1);">
                </div>
            </div>
        `;
    } else if (file.type === 'text/plain' || file.name.endsWith('.txt')) {
        // Text file with better formatting
        container.innerHTML = `
            <div class="text-modal-viewer">
                <div class="text-toolbar">
                    <button class="pdf-tool-btn" onclick="copyTextContent()">📋 Copy All</button>
                    <button class="pdf-tool-btn" onclick="openFileInNewWindow(${taskId}, ${fileIndex})">🔗 Open in New Tab</button>
                </div>
                <pre id="textFileContent" class="text-file-content">${escapeHtml(file.textContent || file.content)}</pre>
            </div>
        `;
    } else {
        // Other files - download option
        container.innerHTML = `
            <div class="generic-file-viewer">
                <p class="file-icon-large">📄</p>
                <p class="file-name-display">${file.name}</p>
                <p class="file-type-info">This file type cannot be previewed</p>
                <a href="${file.data}" download="${file.name}" class="btn-primary">⬇️ Download File</a>
            </div>
        `;
    }
    
    document.getElementById('studyViewer').classList.remove('hidden');
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function openFileInNewWindow(taskId, fileIndex) {
    const file = taskFiles[taskId][fileIndex];
    if (file.data) {
        const newWindow = window.open('', '_blank');
        if (file.type === 'application/pdf' || file.name.endsWith('.pdf')) {
            newWindow.document.write(`
                <!DOCTYPE html>
                <html>
                <head><title>${file.name} - StudyFlow</title></head>
                <body style="margin:0;padding:0;height:100vh;">
                    <embed src="${file.data}" type="application/pdf" style="width:100%;height:100%;">
                </body>
                </html>
            `);
        } else if (file.type.startsWith('image/')) {
            newWindow.document.write(`
                <!DOCTYPE html>
                <html>
                <head><title>${file.name} - StudyFlow</title>
                <style>body{margin:0;background:#1a1f2e;display:flex;align-items:center;justify-content:center;min-height:100vh;}img{max-width:100%;max-height:100vh;}</style>
                </head>
                <body><img src="${file.data}" alt="${file.name}"></body>
                </html>
            `);
        } else if (file.type === 'text/plain' || file.name.endsWith('.txt')) {
            newWindow.document.write(`
                <!DOCTYPE html>
                <html>
                <head><title>${file.name} - StudyFlow</title>
                <style>body{margin:0;padding:20px;background:#1a1f2e;color:#f0f6fc;font-family:monospace;}pre{white-space:pre-wrap;word-wrap:break-word;}</style>
                </head>
                <body><pre>${escapeHtml(file.textContent || file.content)}</pre></body>
                </html>
            `);
        }
    }
}

let currentZoom = 1;

function zoomImage(delta) {
    currentZoom = Math.max(0.5, Math.min(3, currentZoom + delta));
    const img = document.getElementById('zoomableImage');
    if (img) img.style.transform = `scale(${currentZoom})`;
}

function resetImageZoom() {
    currentZoom = 1;
    const img = document.getElementById('zoomableImage');
    if (img) img.style.transform = 'scale(1)';
}

function copyTextContent() {
    const textEl = document.getElementById('textFileContent');
    if (textEl) {
        navigator.clipboard.writeText(textEl.textContent).then(() => {
            showNotification('📋', 'Copied!', 'Text content copied to clipboard');
        });
    }
}

function toggleFullscreenViewer() {
    const viewer = document.getElementById('studyViewer');
    viewer.classList.toggle('fullscreen-viewer');
}

function closeStudyViewer() {
    document.getElementById('studyViewer').classList.add('hidden');
    document.getElementById('studyViewer').classList.remove('fullscreen-viewer');
}

async function removeFile(taskId, fileIndex, fileId) {
    // Delete from server if it has an ID
    if (fileId) {
        await deleteFileFromServer(taskId, fileId);
    }
    taskFiles[taskId].splice(fileIndex, 1);
    displayUploadedFiles(taskId);
}


// ==================== TASK CRUD ====================
document.getElementById('createTaskForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const title = document.getElementById('taskTitle').value;
    const description = document.getElementById('taskDescription').value;
    const estimatedMinutes = document.getElementById('taskDuration').value;
    const priority = document.getElementById('taskPriority').value;

    try {
        const response = await authenticatedFetch('/api/tasks', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, description, estimatedMinutes: parseInt(estimatedMinutes), priority })
        });

        if (response && response.ok) {
            document.getElementById('createTaskForm').reset();
            await loadTasks();
        } else {
            alert('Failed to create task');
        }
    } catch (error) {
        console.error('Failed to create task:', error);
    }
});

async function completeTask(taskId, taskTitle) {
    if (!confirm(`Mark "${taskTitle || 'this task'}" as complete?`)) return;
    
    // Store old progress and achievements for comparison
    let oldProgress = previousProgress ? { ...previousProgress } : null;
    let oldAchievements = null;
    try {
        const achResponse = await authenticatedFetch('/api/progress/achievements');
        if (achResponse && achResponse.ok) {
            oldAchievements = await achResponse.json();
        }
    } catch (e) {}
    
    try {
        const response = await authenticatedFetch(`/api/tasks/${taskId}/complete`, { method: 'PUT' });
        if (response && response.ok) {
            // Refresh tasks and stats first
            await loadTasks();
            await loadStats();
            
            // Show congratulation popup
            showNotification('🎉', 'Task Completed!', `Great job! You've completed "${taskTitle || 'your task'}"!`);
            
            // Show XP gained popup
            if (oldProgress && previousProgress) {
                const xpGained = previousProgress.totalXp - oldProgress.totalXp;
                if (xpGained > 0) {
                    setTimeout(() => {
                        showNotification('⭐', 'XP Earned!', `+${xpGained} XP for completing the task!`);
                    }, 2000);
                }
                
                // Check for level up
                if (previousProgress.currentLevel > oldProgress.currentLevel) {
                    setTimeout(() => {
                        showNotification('⬆️', 'Level Up!', `You reached Level ${previousProgress.currentLevel}: ${LEVEL_TITLES[previousProgress.currentLevel - 1]}!`);
                    }, 4000);
                }
            }
            
            // Check for new achievements
            try {
                const newAchResponse = await authenticatedFetch('/api/progress/achievements');
                if (newAchResponse && newAchResponse.ok) {
                    const newAchievements = await newAchResponse.json();
                    checkForNewAchievements(oldAchievements, newAchievements);
                }
            } catch (e) {}
        }
    } catch (error) {
        console.error('Failed to complete task:', error);
    }
}

async function uncompleteTask(taskId) {
    if (!confirm('Restore this task to active?')) return;
    
    try {
        const response = await authenticatedFetch(`/api/tasks/${taskId}/uncomplete`, { method: 'PUT' });
        if (response && response.ok) {
            await loadTasks();
        }
    } catch (error) {
        console.error('Failed to restore task:', error);
    }
}

async function deleteTask(taskId) {
    if (!confirm('Delete this task?')) return;
    try {
        const response = await authenticatedFetch(`/api/tasks/${taskId}`, { method: 'DELETE' });
        if (response && response.ok) await loadTasks();
    } catch (error) {
        console.error('Failed to delete task:', error);
    }
}

// ==================== ACHIEVEMENTS ====================
async function loadAchievements() {
    try {
        const response = await authenticatedFetch('/api/progress/achievements');
        if (response && response.ok) {
            const achievements = await response.json();
            displayAchievements(achievements);
        }
    } catch (error) {
        console.error('Failed to load achievements:', error);
    }
}

function displayAchievements(achievements) {
    const container = document.getElementById('achievementsGrid');
    container.innerHTML = '';

    const achievementInfo = {
        'first_steps': { icon: '👣', hint: 'Complete your first study session' },
        'early_bird': { icon: '🌅', hint: 'Study before 9 AM' },
        'night_owl': { icon: '🦉', hint: 'Study after 10 PM' },
        'marathon': { icon: '🏃', hint: 'Complete 10 sessions in one day' },
        'consistency_king': { icon: '👑', hint: 'Maintain a 7-day streak' },
        'centurion': { icon: '💯', hint: 'Complete 100 total sessions' },
        'task_master': { icon: '✅', hint: 'Complete 50 tasks' },
        'speed_demon': { icon: '⚡', hint: 'Complete 5 tasks in one day' },
        'focused': { icon: '🎯', hint: 'Complete 4 consecutive work sessions' },
        'level_5': { icon: '⭐', hint: 'Reach level 5' }
    };

    achievements.forEach(ach => {
        // Get achievement info with fallback
        const info = achievementInfo[ach.achievementKey];
        
        // Determine icon to display
        let displayIcon;
        if (ach.isUnlocked) {
            displayIcon = info ? info.icon : '🏆';
        } else {
            displayIcon = '🔒';
        }
        
        // Get hint text
        const hintText = info ? info.hint : (ach.description || 'Complete this achievement');
        
        const achEl = document.createElement('div');
        achEl.className = `achievement-card ${ach.isUnlocked ? 'unlocked' : ''}`;
        achEl.innerHTML = `
            <div class="achievement-icon">${displayIcon}</div>
            <div class="achievement-name">${ach.name}</div>
            <div class="achievement-xp">+${ach.xpReward} XP</div>
            <div class="achievement-description">${ach.isUnlocked ? '✓ Unlocked!' : hintText}</div>
        `;
        container.appendChild(achEl);
    });
}

// ==================== STATISTICS ====================
async function loadStatistics() {
    try {
        // Load weekly chart
        const weeklyResponse = await authenticatedFetch('/api/statistics/weekly');
        if (weeklyResponse && weeklyResponse.ok) {
            const weeklyData = await weeklyResponse.json();
            displayWeeklyChart(weeklyData);
        } else {
            displayWeeklyChart([]);
        }

        // Load overall statistics
        const statsResponse = await authenticatedFetch('/api/statistics');
        if (statsResponse && statsResponse.ok) {
            const stats = await statsResponse.json();
            displayStatsSummary(stats);
            displaySessionHistory(stats.recentSessions || []);
        }
    } catch (error) {
        console.error('Failed to load statistics:', error);
        displayWeeklyChart([]);
    }
}

function displayStatsSummary(stats) {
    document.getElementById('totalStudyHours').textContent = (stats.totalStudyHours || 0).toFixed(1);
    document.getElementById('todaySessions').textContent = stats.todaySessions || 0;
    document.getElementById('weekSessions').textContent = stats.weekSessions || 0;
    document.getElementById('completedTasksCount').textContent = stats.completedTasks || 0;
}

function displaySessionHistory(sessions) {
    const container = document.getElementById('sessionHistoryList');
    container.innerHTML = '';

    if (sessions.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: var(--text-muted); padding: 40px;">No sessions yet. Start studying to see your history! 📖</p>';
        return;
    }

    sessions.forEach(session => {
        const sessionEl = document.createElement('div');
        sessionEl.className = 'history-item';
        
        const sessionIcon = session.sessionType === 'WORK' ? '📚' : 
                           session.sessionType === 'SHORT_BREAK' ? '☕' : '🌴';
        const sessionName = session.sessionType === 'WORK' ? 'Focus Session' : 
                           session.sessionType === 'SHORT_BREAK' ? 'Short Break' : 'Long Break';
        
        const date = new Date(session.completedAt);
        const timeStr = date.toLocaleString('en-US', { 
            month: 'short', 
            day: 'numeric', 
            hour: '2-digit', 
            minute: '2-digit' 
        });

        sessionEl.innerHTML = `
            <div class="history-item-info">
                <div class="history-item-icon">${sessionIcon}</div>
                <div class="history-item-details">
                    <h4>${sessionName}</h4>
                    <p>${timeStr}</p>
                </div>
            </div>
            <div class="history-item-stats">
                <div class="history-item-duration">${session.durationMinutes} min</div>
                <div class="history-item-xp">+${session.xpEarned} XP</div>
            </div>
        `;
        container.appendChild(sessionEl);
    });
}

function displayWeeklyChart(data) {
    const container = document.getElementById('weeklyChartContainer');
    container.innerHTML = '';
    
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const maxMinutes = Math.max(...data.map(d => d.minutes || 0), 30);

    days.forEach((day, index) => {
        const dayData = data[index] || { minutes: 0 };
        const height = Math.max((dayData.minutes / maxMinutes) * 100, 5);
        
        const barEl = document.createElement('div');
        barEl.className = 'chart-bar';
        barEl.style.height = `${height}%`;
        barEl.innerHTML = `<div class="chart-bar-label">${day}</div>`;
        barEl.title = `${dayData.minutes || 0} minutes`;
        container.appendChild(barEl);
    });
}

// ==================== NAVIGATION ====================
document.getElementById('navHome').addEventListener('click', () => {
    showSection('homeSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navHome').classList.add('active');
    loadStats();
});

document.getElementById('navTimer').addEventListener('click', () => {
    showSection('timerSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navTimer').classList.add('active');
});

document.getElementById('navTasks').addEventListener('click', () => {
    showSection('tasksSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navTasks').classList.add('active');
    loadTasks();
});

document.getElementById('navAchievements').addEventListener('click', () => {
    showSection('achievementsSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navAchievements').classList.add('active');
    loadAchievements();
});

document.getElementById('navStats').addEventListener('click', () => {
    showSection('statsSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navStats').classList.add('active');
    loadStatistics();
});

document.getElementById('navHelp').addEventListener('click', () => {
    showSection('helpSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navHelp').classList.add('active');
});

document.getElementById('navFeedback').addEventListener('click', () => {
    showSection('feedbackSection');
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById('navFeedback').classList.add('active');
});

document.getElementById('logoutBtn').addEventListener('click', logout);

// ==================== FEEDBACK ====================
document.getElementById('feedbackForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const feedbackData = {
        type: document.getElementById('feedbackType').value,
        subject: document.getElementById('feedbackSubject').value,
        message: document.getElementById('feedbackMessage').value,
        email: document.getElementById('feedbackEmail').value || null,
        username: currentUser ? currentUser.username : 'Anonymous'
    };
    
    try {
        const response = await authenticatedFetch('/api/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(feedbackData)
        });
        
        if (response && response.ok) {
            showSuccess('feedbackSuccess', '✅ Thank you! Your feedback has been sent successfully.');
            document.getElementById('feedbackForm').reset();
            showNotification('💬', 'Feedback Sent!', 'Thank you for helping us improve StudyFlow!');
        } else {
            showError('feedbackError', 'Failed to send feedback. Please try again.');
        }
    } catch (error) {
        console.error('Failed to send feedback:', error);
        showError('feedbackError', 'Failed to send feedback. Please try again.');
    }
});

// Initialize
window.addEventListener('DOMContentLoaded', () => {
    resetTimer();
});
