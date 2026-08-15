document.addEventListener('DOMContentLoaded', () => {
    const uploadZone = document.getElementById('uploadZone');
    const fileInput = document.getElementById('fileInput');
    const fileList = document.getElementById('fileList');
    const uploadBtn = document.getElementById('uploadBtn');
    const uploadForm = document.getElementById('uploadForm');
    const uploadProgress = document.getElementById('uploadProgress');
    let selectedFiles = [];
    let currentPage = parseInt(document.body.dataset.page || '0');
    let hasMore = document.body.dataset.hasMore === 'true';
    let currentFilter = document.body.dataset.filter || '';
    let isLoggedIn = document.body.dataset.loggedIn === 'true';
    let isLoadingMore = false;

    // --- Upload Zone ---
    uploadZone.addEventListener('click', () => fileInput.click());

    uploadZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadZone.classList.add('drag-over');
    });

    uploadZone.addEventListener('dragleave', () => {
        uploadZone.classList.remove('drag-over');
    });

    uploadZone.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadZone.classList.remove('drag-over');
        handleFiles(e.dataTransfer.files);
    });

    fileInput.addEventListener('change', () => {
        handleFiles(fileInput.files);
    });

    function handleFiles(files) {
        for (const file of files) {
            if (!selectedFiles.find(f => f.name === file.name && f.size === file.size)) {
                selectedFiles.push(file);
            }
        }
        renderFileList();
    }

    function renderFileList() {
        fileList.innerHTML = '';
        selectedFiles.forEach((file, index) => {
            const isImage = file.type.startsWith('image/');
            const icon = isImage ? 'bi-image' : 'bi-camera-video';
            const size = formatSize(file.size);
            const item = document.createElement('div');
            item.className = 'file-item';
            item.innerHTML = `
                <i class="bi ${icon} file-icon"></i>
                <span class="file-name">${escapeHtml(file.name)}</span>
                <span class="file-size">${size}</span>
                <button class="remove-file" data-index="${index}"><i class="bi bi-x-lg"></i></button>
            `;
            fileList.appendChild(item);
        });

        fileList.querySelectorAll('.remove-file').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const idx = parseInt(btn.dataset.index);
                selectedFiles.splice(idx, 1);
                renderFileList();
            });
        });

        uploadBtn.disabled = selectedFiles.length === 0;
    }

    function formatSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // --- Upload (auto on file select, XHR with progress) ---
    let isUploading = false;

    uploadForm.addEventListener('submit', function(e) {
        if (selectedFiles.length === 0) { e.preventDefault(); return; }
        e.preventDefault();
        if (isUploading) return;
        isUploading = true;

        uploadBtn.disabled = true;
        uploadBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-1"></div>Uploading...';
        uploadProgress.classList.remove('d-none');

        const files = selectedFiles.slice();
        const formData = new FormData();
        files.forEach(f => formData.append('files', f));

        const xhr = new XMLHttpRequest();
        xhr.open('POST', uploadForm.action, true);

        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                const pct = Math.round((e.loaded / e.total) * 100);
                const bar = uploadProgress.querySelector('.progress-bar');
                bar.style.width = pct + '%';
                bar.textContent = pct + '%';
            }
        });

        xhr.upload.onload = function() {
            const modal = bootstrap.Modal.getInstance(document.getElementById('uploadModal'));
            if (modal) modal.hide();
        };

        xhr.onload = function() {
            if (xhr.status >= 200 && xhr.status < 400) {
                window.location.href = '/';
            } else if (xhr.status === 401 || xhr.status === 403) {
                window.location.href = '/login';
            } else {
                showNotification('Upload failed (HTTP ' + xhr.status + ').', 'danger');
            }
            isUploading = false;
        };

        xhr.onerror = function() {
            showNotification('Upload failed. Check your connection.', 'danger');
            isUploading = false;
            uploadBtn.innerHTML = '<i class="bi bi-upload me-1"></i>Upload';
            uploadBtn.disabled = false;
            uploadProgress.classList.add('d-none');
        };

        xhr.ontimeout = function() {
            showNotification('Upload timed out. Files may be too large.', 'danger');
            isUploading = false;
            uploadBtn.innerHTML = '<i class="bi bi-upload me-1"></i>Upload';
            uploadBtn.disabled = false;
            uploadProgress.classList.add('d-none');
        };

        xhr.send(formData);
    });

    // --- Auto-upload: start immediately when files are selected ---
    let autoUploadTimer = null;

    function triggerAutoUpload() {
        if (autoUploadTimer) clearTimeout(autoUploadTimer);
        autoUploadTimer = setTimeout(() => {
            if (selectedFiles.length > 0) {
                uploadForm.dispatchEvent(new Event('submit'));
            }
        }, 200);
    }

    // Patch handleFiles to auto-upload
    const originalHandleFiles = handleFiles;
    handleFiles = function(files) {
        originalHandleFiles(files);
        triggerAutoUpload();
    };

    function showNotification(msg, type) {
        type = type || 'success';
        const container = document.getElementById('notificationContainer');
        if (!container) return;
        const alert = document.createElement('div');
        alert.className = 'alert alert-' + type + ' alert-dismissible fade show';
        alert.role = 'alert';
        alert.innerHTML = '<i class="bi bi-' + (type === 'danger' ? 'exclamation-triangle' : type === 'info' ? 'info-circle' : 'check-circle') + ' me-2"></i>' +
            msg +
            '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
        container.appendChild(alert);
        setTimeout(() => { alert.classList.remove('show'); setTimeout(() => alert.remove(), 300); }, 4000);
    }

    function showUploadError(msg) {
        uploadBtn.innerHTML = '<i class="bi bi-upload me-1"></i>Upload';
        uploadBtn.disabled = false;
        uploadProgress.classList.add('d-none');
        const bar = uploadProgress.querySelector('.progress-bar');
        bar.style.width = '0%';
        bar.textContent = '';
        alert(msg);
    }

    // --- Lightbox navigation ---
    let lightboxTriggers = [];
    let lightboxIndex = 0;
    let lightboxModalInstance = null;

    function getLightboxTriggers() {
        return Array.from(document.querySelectorAll('.lightbox-trigger'));
    }

    function loadLightboxItem(index) {
        const triggers = getLightboxTriggers();
        if (index < 0 || index >= triggers.length) return;
        lightboxIndex = index;
        const t = triggers[index];
        openLightbox(t.dataset.src, t.dataset.type, t.dataset.filename, t.dataset.size, false);
    }

    document.addEventListener('click', (e) => {
        const trigger = e.target.closest('.lightbox-trigger');
        if (!trigger) return;
        e.preventDefault();
        lightboxTriggers = getLightboxTriggers();
        lightboxIndex = lightboxTriggers.indexOf(trigger);
        openLightbox(trigger.dataset.src, trigger.dataset.type,
                    trigger.dataset.filename, trigger.dataset.size, true);
    });

    function openLightbox(src, type, filename, size, show) {
        const img = document.getElementById('lightboxImg');
        const video = document.getElementById('lightboxVideo');
        const filenameEl = document.getElementById('lightboxFilename');
        const sizeEl = document.getElementById('lightboxSize');
        const downloadLink = document.getElementById('lightboxDownload');

        filenameEl.textContent = filename;
        sizeEl.textContent = size;
        downloadLink.href = src;
        downloadLink.download = filename;

        if (type === 'image') {
            img.src = src;
            img.classList.remove('d-none');
            video.classList.add('d-none');
            video.pause();
        } else {
            video.src = src;
            video.classList.remove('d-none');
            img.classList.add('d-none');
        }

        updateArrowVisibility();
        if (show) {
            lightboxModalInstance = lightboxModalInstance || new bootstrap.Modal(document.getElementById('lightboxModal'));
            lightboxModalInstance.show();
        }
    }

    function updateArrowVisibility() {
        const triggers = getLightboxTriggers();
        const prev = document.getElementById('lightboxPrev');
        const next = document.getElementById('lightboxNext');
        if (triggers.length <= 1) {
            prev.classList.add('d-none');
            next.classList.add('d-none');
        } else {
            prev.classList.toggle('d-none', lightboxIndex <= 0);
            next.classList.toggle('d-none', lightboxIndex >= triggers.length - 1);
        }
    }

    document.getElementById('lightboxPrev').addEventListener('click', () => {
        if (lightboxIndex > 0) loadLightboxItem(lightboxIndex - 1);
    });

    document.getElementById('lightboxNext').addEventListener('click', () => {
        const triggers = getLightboxTriggers();
        if (lightboxIndex < triggers.length - 1) loadLightboxItem(lightboxIndex + 1);
    });

    document.addEventListener('keydown', (e) => {
        const modal = document.getElementById('lightboxModal');
        if (!modal.classList.contains('show')) return;
        if (e.key === 'ArrowLeft') {
            if (lightboxIndex > 0) loadLightboxItem(lightboxIndex - 1);
        } else if (e.key === 'ArrowRight') {
            const triggers = getLightboxTriggers();
            if (lightboxIndex < triggers.length - 1) loadLightboxItem(lightboxIndex + 1);
        } else if (e.key === 'Escape') {
            lightboxModalInstance.hide();
        }
    });

    document.getElementById('lightboxModal').addEventListener('hidden.bs.modal', () => {
        const video = document.getElementById('lightboxVideo');
        video.pause();
        video.src = '';
        document.getElementById('lightboxImg').src = '';
    });

    // --- Load More ---
    const loadMoreBtn = document.getElementById('loadMoreBtn');
    const loadMoreContainer = document.getElementById('loadMoreContainer');
    const loadingSpinner = document.getElementById('loadingSpinner');

    if (loadMoreBtn) {
        loadMoreBtn.addEventListener('click', () => {
            if (isLoadingMore || !hasMore) return;
            isLoadingMore = true;

            loadMoreContainer.classList.add('d-none');
            loadingSpinner.classList.remove('d-none');

            currentPage++;
            const url = '/api/media/page?page=' + currentPage +
                        (currentFilter ? '&filter=' + currentFilter : '');

            fetch(url)
                .then(r => r.json())
                .then(data => {
                    const grid = document.getElementById('galleryGrid');
                    data.content.forEach(media => {
                        grid.insertAdjacentHTML('beforeend', buildCardHTML(media));
                    });

                    hasMore = data.hasMore;
                    isLoadingMore = false;
                    loadingSpinner.classList.add('d-none');

                    if (hasMore) {
                        loadMoreContainer.classList.remove('d-none');
                    }
                    startCompressionPolling();
                })
                .catch(() => {
                    isLoadingMore = false;
                    loadingSpinner.classList.add('d-none');
                    loadMoreContainer.classList.remove('d-none');
                });
        });
    }

    function buildCardHTML(media) {
        const isImage = media.mediaType === 'IMAGE';
        const fileUrl = '/uploads/' + media.storedFilename;
        const thumbUrl = media.thumbnailFilename
            ? '/uploads/thumbs/' + media.thumbnailFilename
            : fileUrl;

        const deleteBtn = isLoggedIn
            ? `<button class="btn btn-sm btn-outline-danger delete-btn" data-id="${media.id}"><i class="bi bi-trash3"></i></button>`
            : '';

        const shared = `
            <div class="gallery-card-select">
                <input type="checkbox" class="form-check-input media-select" data-id="${media.id}">
            </div>
            <div class="${isImage ? 'gallery-thumb-wrap' : 'gallery-thumb-wrap video-thumbnail'}">
                ${isImage
                    ? `<img src="${thumbUrl}" alt="${escapeHtml(media.originalFilename)}" class="card-img-top gallery-img" loading="lazy">`
                    : `<video src="${fileUrl}" muted preload="metadata" class="card-img-top gallery-img"></video>
                       <div class="video-play-btn"><i class="bi bi-play-fill"></i></div>`}
            </div>
            <div class="gallery-overlay"><i class="bi bi-arrows-fullscreen"></i></div>`;

        const buttons = `
            <button class="btn btn-sm btn-outline-primary collection-add-btn" data-id="${media.id}" title="Add to collection"><i class="bi bi-folder-plus"></i></button>
            <button class="btn btn-sm btn-outline-secondary download-single-btn" data-filename="${escapeHtml(media.originalFilename)}" data-url="${fileUrl}" title="Download"><i class="bi bi-download"></i></button>
            ${deleteBtn}`;

        const compressionBadge = isImage ? '' : (media.compressed
            ? '<span class="compression-badge badge bg-success"><i class="bi bi-check-circle"></i> Compressed</span>'
            : '<span class="compression-badge badge bg-warning text-dark"><i class="bi bi-arrow-repeat"></i> Compressing...</span>');

        return `
            <div class="gallery-item" data-id="${media.id}" data-type="${media.mediaType}"${isImage ? '' : ' data-compressed="' + media.compressed + '"'}>
                <div class="card gallery-card h-100">
                    <a href="#" class="lightbox-trigger" data-src="${fileUrl}" data-type="${isImage ? 'image' : 'video'}"
                       data-filename="${escapeHtml(media.originalFilename)}" data-size="${media.fileSizeFormatted}">
                        ${shared}
                    </a>
                    <div class="card-body py-2 px-2">
                        <p class="card-text small text-truncate mb-0">${escapeHtml(media.originalFilename)}</p>
                        <div class="d-flex justify-content-between align-items-center">
                            <small class="text-muted">${media.fileSizeFormatted}</small>
                            <div class="d-flex gap-1 align-items-center">${compressionBadge}${buttons}</div>
                        </div>
                    </div>
                </div>
            </div>`;
    }

    // --- Multi-select ---
    let selectedIds = new Set();
    const bulkToolbar = document.getElementById('bulkToolbar');
    const selectedCount = document.getElementById('selectedCount');
    const downloadSelectedBtn = document.getElementById('downloadSelectedBtn');
    const deleteSelectedBtn = document.getElementById('deleteSelectedBtn');
    const clearSelectionBtn = document.getElementById('clearSelectionBtn');

    document.addEventListener('change', (e) => {
        const cb = e.target.closest('.media-select');
        if (!cb) return;
        const id = cb.dataset.id;
        if (cb.checked) {
            selectedIds.add(id);
            cb.closest('.gallery-item').classList.add('selected');
        } else {
            selectedIds.delete(id);
            cb.closest('.gallery-item').classList.remove('selected');
        }
        updateBulkToolbar();
    });

    function updateBulkToolbar() {
        const count = selectedIds.size;
        if (count === 0) {
            bulkToolbar.classList.add('d-none');
            bulkToolbar.classList.remove('d-flex');
            return;
        }
        bulkToolbar.classList.remove('d-none');
        bulkToolbar.classList.add('d-flex');
        selectedCount.textContent = count + ' selected';
    }

    if (clearSelectionBtn) {
        clearSelectionBtn.addEventListener('click', clearSelection);
    }

    function clearSelection() {
        document.querySelectorAll('.media-select:checked').forEach(cb => {
            cb.checked = false;
            cb.closest('.gallery-item').classList.remove('selected');
        });
        selectedIds.clear();
        updateBulkToolbar();
    }

    if (downloadSelectedBtn) {
        downloadSelectedBtn.addEventListener('click', () => {
            if (selectedIds.size === 0) return;
            const ids = Array.from(selectedIds).join(',');
            const a = document.createElement('a');
            a.href = '/download/selected?ids=' + ids;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            clearSelection();
        });
    }

    if (deleteSelectedBtn) {
        deleteSelectedBtn.addEventListener('click', () => {
            if (selectedIds.size === 0) return;
            if (!confirm('Delete ' + selectedIds.size + ' selected file(s)? This cannot be undone.')) return;
            const ids = Array.from(selectedIds).map(Number);
            fetch('/media/batch-delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(ids)
            })
            .then(r => {
                if (r.status === 401 || r.status === 403) { window.location.href = '/login'; return null; }
                return r.json();
            })
            .then(data => {
                if (!data) return;
                document.querySelectorAll('.media-select:checked').forEach(cb => {
                    const item = cb.closest('.gallery-item');
                    if (item) item.remove();
                });
                clearSelection();
            })
            .catch(() => alert('Failed to delete selected files.'));
        });
    }

    // --- Single file download ---
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.download-single-btn');
        if (!btn) return;
        e.preventDefault();
        const url = btn.dataset.url;
        const a = document.createElement('a');
        a.href = url;
        a.download = btn.dataset.filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    });

    // --- Delete (event delegation) ---
    let deleteId = null;
    const deleteModalEl = document.getElementById('deleteModal');
    const deleteModal = deleteModalEl ? new bootstrap.Modal(deleteModalEl) : null;

    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.delete-btn');
        if (!btn) return;
        e.preventDefault();
        deleteId = btn.dataset.id;
        if (deleteModal) deleteModal.show();
    });

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (!deleteId) return;

        fetch('/media/' + deleteId, { method: 'DELETE' })
            .then(r => {
                if (r.status === 401 || r.status === 403) {
                    window.location.href = '/login';
                    return null;
                }
                return r.json();
            })
            .then(data => {
                if (!data) return;
                const item = document.querySelector(`.gallery-item[data-id="${deleteId}"]`);
                if (item) {
                    item.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                    item.style.opacity = '0';
                    item.style.transform = 'scale(0.9)';
                    setTimeout(() => item.remove(), 300);
                }
                deleteModal.hide();
                deleteId = null;
            })
            .catch(() => {
                alert('Failed to delete. Please try again.');
                deleteModal.hide();
            });
    });

    // --- Create Collection ---
    const createCollectionBtn = document.getElementById('createCollectionBtn');
    if (createCollectionBtn) {
        createCollectionBtn.addEventListener('click', () => {
            const name = document.getElementById('collectionName').value.trim();
            const desc = document.getElementById('collectionDesc').value.trim();
            if (!name) { alert('Please enter a collection name.'); return; }

            const formData = new FormData();
            formData.append('name', name);
            if (desc) formData.append('description', desc);

            fetch('/collections', { method: 'POST', body: formData })
                .then(r => {
                    if (r.status === 401 || r.status === 403) { window.location.href = '/login'; return null; }
                    return r.json();
                })
                .then(collection => {
                    if (!collection) return;
                    window.location.href = '/?collectionId=' + collection.id;
                })
                .catch(() => alert('Failed to create collection.'));
        });
    }

    // --- Add to Collection (per-card button) ---
    let addMediaId = null;
    const addToCollectionModalEl = document.getElementById('addToCollectionModal');
    const addToCollectionModal = addToCollectionModalEl ? new bootstrap.Modal(addToCollectionModalEl) : null;

    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.collection-add-btn');
        if (!btn) return;
        e.preventDefault();
        addMediaId = btn.dataset.id;
        renderCollectionPicker();
        if (addToCollectionModal) addToCollectionModal.show();
    });

    function renderCollectionPicker() {
        const list = document.getElementById('collectionPickerList');
        const collections = window.__collections || [];
        if (collections.length === 0) {
            list.innerHTML = '<div class="list-group-item text-muted">No collections yet. Create one first!</div>';
            return;
        }
        list.innerHTML = '';
        collections.forEach(col => {
            const item = document.createElement('button');
            item.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
            item.innerHTML = '<span><i class="bi bi-folder2 me-2"></i>' + escapeHtml(col.name) + '</span>' +
                '<span class="badge bg-secondary">' + col.mediaCount + '</span>';
            item.addEventListener('click', () => {
                const fd = new FormData();
                fd.append('mediaId', addMediaId);
                fetch('/collections/' + col.id + '/add-media', { method: 'POST', body: fd })
                    .then(r => {
                        if (r.status === 401 || r.status === 403) { window.location.href = '/login'; return null; }
                        return r.json();
                    })
                    .then(data => {
                        if (!data) return;
                        if (addToCollectionModal) addToCollectionModal.hide();
                        addMediaId = null;
                    })
                    .catch(() => alert('Failed to add to collection.'));
            });
            list.appendChild(item);
        });
    }

    // --- Compression status polling ---
    let compressionPollTimer = null;

    function pollVideoCompression() {
        const pending = document.querySelectorAll('.gallery-item[data-type="VIDEO"][data-compressed="false"]');
        if (pending.length === 0) {
            if (compressionPollTimer) {
                clearInterval(compressionPollTimer);
                compressionPollTimer = null;
            }
            return;
        }
        pending.forEach(el => {
            const id = el.dataset.id;
            fetch('/media/' + id)
                .then(r => r.json())
                .then(media => {
                    if (media.compressed) {
                        el.dataset.compressed = 'true';
                        const badge = el.querySelector('.compression-badge');
                        if (badge) {
                            badge.className = 'compression-badge badge bg-success';
                            badge.innerHTML = '<i class="bi bi-check-circle"></i> Compressed';
                        }
                        const fileUrl = '/uploads/' + media.storedFilename;
                        const trigger = el.querySelector('.lightbox-trigger');
                        if (trigger) {
                            trigger.dataset.src = fileUrl;
                            const video = trigger.querySelector('video');
                            if (video) {
                                const wasPaused = video.paused;
                                const playTime = video.currentTime;
                                video.src = fileUrl;
                                if (!wasPaused) video.play();
                                else video.currentTime = playTime;
                            }
                        }
                        const downloadBtn = el.querySelector('.download-single-btn');
                        if (downloadBtn) downloadBtn.dataset.url = fileUrl;
                    }
                })
                .catch(() => {});
        });
    }

    function startCompressionPolling() {
        const pending = document.querySelectorAll('.gallery-item[data-type="VIDEO"][data-compressed="false"]');
        if (pending.length === 0) return;
        if (compressionPollTimer) return;
        compressionPollTimer = setInterval(pollVideoCompression, 3000);
        pollVideoCompression();
    }

    startCompressionPolling();

    // --- Backup Settings (Google Drive toggle per collection) ---
    document.querySelectorAll('.backup-toggle').forEach(toggle => {
        toggle.addEventListener('change', (e) => {
            const colId = e.target.dataset.colId;
            const enabled = e.target.checked;

            const fd = new FormData();
            fd.append('enabled', enabled);

            fetch('/collections/' + colId + '/backup', { method: 'POST', body: fd })
                .then(r => {
                    if (r.status === 401 || r.status === 403) { window.location.href = '/login'; return null; }
                    return r.json();
                })
                .then(data => {
                    if (!data) return;
                    const label = e.target.closest('.d-flex').querySelector('.text-muted');
                    if (label) {
                        label.textContent = data.backupEnabled ? 'Backing up to Drive' : 'Not backing up';
                    }
                })
                .catch(() => {
                    e.target.checked = !enabled;
                    alert('Failed to update backup settings.');
                });
        });
    });
});
