const TEMPLATE = document.createElement("template");

TEMPLATE.innerHTML = `
<style>
    :host {
        display: block;
        font-family: var(--lumo-font-family, Arial, sans-serif);
        color: var(--lumo-body-text-color, #222);
    }

    * {
        box-sizing: border-box;
    }

    .drop {
        border: 2px dashed var(--lumo-contrast-30pct, #aab2bd);
        border-radius: 12px;
        padding: 24px;
        text-align: center;
        background: var(--lumo-contrast-5pct, #f8f9fa);
        transition: 0.2s;
        cursor: pointer;
    }

    .drop.drag {
        border-color: var(--lumo-primary-color, #1676f3);
        background: var(--lumo-primary-color-10pct, #eef5ff);
    }

    .drop.disabled {
        opacity: 0.6;
        cursor: not-allowed;
    }

    .title {
        font-weight: 700;
        font-size: 1rem;
    }

    .hint {
        margin-top: 6px;
        font-size: 0.86rem;
        color: var(--lumo-secondary-text-color, #667);
    }

    .choose {
        margin-top: 14px;
        border: 0;
        border-radius: 8px;
        padding: 10px 16px;
        background: var(--lumo-primary-color, #1676f3);
        color: white;
        font-weight: 600;
        cursor: pointer;
    }

    .choose:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .list {
        display: grid;
        gap: 10px;
        margin-top: 14px;
    }

    .completed-section {
        display: none;
        margin-top: 14px;
        border: 1px solid var(--lumo-contrast-20pct, #d8dde3);
        border-radius: 10px;
        background: var(--lumo-base-color, #fff);
        overflow: hidden;
    }

    .completed-section.visible {
        display: block;
    }

    .completed-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        padding: 10px 12px;
        background: var(--lumo-contrast-5pct, #f8f9fa);
        cursor: pointer;
        user-select: none;
    }

    .completed-header:hover {
        background: var(--lumo-contrast-10pct, #f0f2f4);
    }

    .completed-title {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        font-weight: 700;
    }

    .completed-chevron {
        display: inline-block;
        transition: transform 180ms ease;
    }

    .completed-section.expanded .completed-chevron {
        transform: rotate(90deg);
    }

    .clear-completed {
        border: 1px solid var(--lumo-contrast-20pct, #ccd2d8);
        border-radius: 6px;
        padding: 6px 10px;
        background: var(--lumo-base-color, #fff);
        color: var(--lumo-body-text-color, #222);
        font-family: inherit;
        cursor: pointer;
    }

    .clear-completed:hover {
        background: var(--lumo-contrast-5pct, #f3f4f5);
    }

    .completed-list {
        display: grid;
        gap: 10px;
        max-height: 0;
        padding: 0 10px;
        overflow: hidden;
        opacity: 0;
        transition: max-height 220ms ease, opacity 180ms ease, padding 220ms ease;
    }

    .completed-section.expanded .completed-list {
        max-height: 5000px;
        padding: 10px;
        opacity: 1;
    }

    .completed-list .card {
        margin: 0;
    }

    .card {
        border: 1px solid var(--lumo-contrast-20pct, #d8dde3);
        border-radius: 10px;
        padding: 12px;
        background: var(--lumo-base-color, #fff);
    }

    .card.completed {
        border-color: var(--lumo-success-color-50pct, #66bb6a);
    }

    .card.failed {
        border-color: var(--lumo-error-color-50pct, #ef5350);
    }

    .card.paused {
        border-color: var(--lumo-warning-color, #f4b400);
    }

    .row {
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .file-header {
        min-width: 0;
    }

    .file-icon {
        flex: 0 0 auto;
        font-size: 1.4rem;
    }

    .name {
        min-width: 0;
        flex: 1;
        overflow: hidden;
        font-weight: 600;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .meta {
        font-size: 0.8rem;
        color: var(--lumo-secondary-text-color, #667);
    }

    .size {
        flex: 0 0 auto;
        white-space: nowrap;
    }

    .status-row {
        justify-content: space-between;
    }

    .status {
        font-weight: 500;
    }

    .stats {
        min-width: 0;
        text-align: right;
    }

    progress {
        width: 100%;
        height: 9px;
        margin: 10px 0;
    }

    .actions {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-top: 10px;
    }

    .actions:empty {
        display: none;
    }

    .actions button {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        border: 1px solid var(--lumo-contrast-20pct, #ccd2d8);
        border-radius: 6px;
        padding: 6px 10px;
        background: var(--lumo-base-color, #fff);
        color: var(--lumo-body-text-color, #222);
        font-family: inherit;
        cursor: pointer;
    }

    .actions button:hover:not(:disabled) {
        background: var(--lumo-contrast-5pct, #f3f4f5);
    }

    .actions button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .actions button.primary {
        border-color: var(--lumo-primary-color, #1676f3);
        color: var(--lumo-primary-text-color, #1676f3);
    }

    .actions button.danger {
        color: var(--lumo-error-text-color, #b00020);
    }

    .error {
        margin-top: 7px;
        font-size: 0.82rem;
        color: var(--lumo-error-text-color, #b00020);
        white-space: normal;
        word-break: break-word;
    }

    .success {
        color: var(--lumo-success-text-color, #137333);
    }

    .warning {
        color: var(--lumo-warning-text-color, #8a5a00);
    }

    .finalizing {
        color: var(--lumo-primary-text-color, #1676f3);
    }

    @media (max-width: 600px) {
        .drop {
            padding: 18px 12px;
        }

        .status-row {
            align-items: flex-start;
            flex-direction: column;
            gap: 4px;
        }

        .stats {
            text-align: left;
        }

        .actions button {
            flex: 1 1 auto;
            justify-content: center;
        }
    }
</style>

<div class="drop" part="drop-zone" tabindex="0">
    <div class="title">
        Drop files here | ទម្លាក់ឯកសារនៅទីនេះ
    </div>

    <div class="hint">
        You can upload one or more files.
    </div>

    <button class="choose" type="button">
        Choose Files | ជ្រើសរើសឯកសារ
    </button>

    <input type="file" hidden multiple>
</div>

<div class="list active-list" part="file-list"></div>

<div class="completed-section" part="completed-section">
    <div class="completed-header" role="button" tabindex="0"
         aria-expanded="false">
        <div class="completed-title">
            <span class="completed-chevron">▶</span>
            <span class="completed-label">Completed (0)</span>
        </div>

        <button class="clear-completed" type="button">
            Clear Completed
        </button>
    </div>

    <div class="completed-list"></div>
</div>
`;

class HaloLargeFileUploader extends HTMLElement {

    constructor() {
        super();

        this.attachShadow({ mode: "open" })
            .appendChild(TEMPLATE.content.cloneNode(true));

        this.endpoint =
            this.getAttribute("endpoint") || "api/uploads";

        this.uploadType = "OTHER";
        this.accept = "";
        this.maxFiles = 10;

        this.requestedChunkSize =
            20 * 1024 * 1024;

        this.maxFileSize =
            20 * 1024 * 1024 * 1024;

        this.verifyChunkChecksum = false;
        this.disabled = false;

        this.items = new Map();
        this.completedExpanded = false;
    }

    connectedCallback() {
        this.drop =
            this.shadowRoot.querySelector(".drop");

        this.input =
            this.shadowRoot.querySelector("input");

        this.list =
            this.shadowRoot.querySelector(".active-list");

        this.completedSection =
            this.shadowRoot.querySelector(".completed-section");

        this.completedHeader =
            this.shadowRoot.querySelector(".completed-header");

        this.completedLabel =
            this.shadowRoot.querySelector(".completed-label");

        this.completedList =
            this.shadowRoot.querySelector(".completed-list");

        this.clearCompletedButton =
            this.shadowRoot.querySelector(".clear-completed");

        this.choose =
            this.shadowRoot.querySelector(".choose");

        this.choose.onclick = event => {
            event.stopPropagation();
            this.openFilePicker();
        };

        this.drop.onclick = event => {
            if (event.target !== this.choose) {
                this.openFilePicker();
            }
        };

        this.drop.onkeydown = event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                this.openFilePicker();
            }
        };

        this.input.onchange = () => {
            this.addFiles([...this.input.files]);
            this.input.value = "";
        };

        ["dragenter", "dragover"].forEach(eventName => {
            this.drop.addEventListener(eventName, event => {
                event.preventDefault();

                if (!this.disabled) {
                    this.drop.classList.add("drag");
                }
            });
        });

        ["dragleave", "drop"].forEach(eventName => {
            this.drop.addEventListener(eventName, event => {
                event.preventDefault();
                this.drop.classList.remove("drag");
            });
        });

        this.drop.addEventListener("drop", event => {
            if (!this.disabled) {
                this.addFiles([...event.dataTransfer.files]);
            }
        });

        this.completedHeader.onclick = event => {
            if (event.target === this.clearCompletedButton) {
                return;
            }

            this.toggleCompleted();
        };

        this.completedHeader.onkeydown = event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                this.toggleCompleted();
            }
        };

        this.clearCompletedButton.onclick = event => {
            event.preventDefault();
            event.stopPropagation();
            this.clearCompleted();
        };

        this._syncProps();
        this._refreshCompletedSection();
    }

    _syncProps() {
        if (!this.input) {
            return;
        }

        this.input.accept = this.accept || "";
        this.input.multiple = this.maxFiles !== 1;
        this.choose.disabled = Boolean(this.disabled);

        this.drop.classList.toggle(
            "disabled",
            Boolean(this.disabled)
        );
    }

    openFilePicker() {
        if (!this.disabled) {
            this.input.click();
        }
    }

    addFiles(files) {
        if (this.disabled || !Array.isArray(files)) {
            return;
        }

        const activeItems = [...this.items.values()]
            .filter(item => item.state !== "cancelled");

        const available = Math.max(
            0,
            this.maxFiles - activeItems.length
        );

        if (available === 0) {
            this._emit("upload-failed", {
                uploadId: "",
                fileName: "",
                message:
                    `Maximum number of files reached: ${this.maxFiles}`
            });

            return;
        }

        files.slice(0, available).forEach(file => {
            if (file.size > this.maxFileSize) {
                this._emit("upload-failed", {
                    uploadId: "",
                    fileName: file.name,
                    message: "File exceeds maximum size"
                });

                return;
            }

            const key = this._fingerprint(file);

            if (this.items.has(key)) {
                return;
            }

            const item = {
                key,
                file,
                uploadId: null,
                state: "queued",
                controller: null,
                uploadedBytes: 0,
                nextChunkIndex: 0,
                chunkSize: this.requestedChunkSize,
                startTime: 0,
                lastSampleTime: 0,
                lastSampleBytes: 0,
                speed: 0,
                eta: 0,
                error: "",
                finalizingMessage: "",
                card: null
            };

            this.items.set(key, item);
            this._render(item);
            this._start(item);
        });
    }

    async _start(item) {
        try {
            item.state = "initializing";
            item.error = "";
            this._update(item);

            const storageKey =
                "halo-upload:" + item.key;

            const savedUploadId =
                localStorage.getItem(storageKey);

            let status = null;

            if (savedUploadId) {
                try {
                    status = await this._json(
                        `${this.endpoint}/${savedUploadId}/status`,
                        {
                            method: "GET",
                            headers: this._headers()
                        }
                    );

                    item.uploadId = savedUploadId;
                } catch (error) {
                    localStorage.removeItem(storageKey);
                    item.uploadId = null;
                }
            }

            if (!status) {
                const init = await this._json(
                    `${this.endpoint}/init`,
                    {
                        method: "POST",
                        headers: this._headers({
                            "Content-Type": "application/json"
                        }),
                        body: JSON.stringify({
                            fileName: item.file.name,
                            mimeType:
                                item.file.type ||
                                "application/octet-stream",
                            fileSize: item.file.size,
                            requestedChunkSize:
                                this.requestedChunkSize,
                            uploadType: this.uploadType,
                            checksumSha256: null
                        })
                    }
                );

                item.uploadId = init.uploadId;
                status = init;

                localStorage.setItem(
                    storageKey,
                    item.uploadId
                );
            }

            item.chunkSize = Number(
                status.chunkSize ||
                this.requestedChunkSize
            );

            item.nextChunkIndex = Number(
                status.nextChunkIndex || 0
            );

            item.uploadedBytes = Number(
                status.uploadedSize || 0
            );

            item.state = "uploading";

            item.startTime = performance.now();
            item.lastSampleTime = item.startTime;
            item.lastSampleBytes = item.uploadedBytes;

            this._emit("upload-started", {
                uploadId: item.uploadId,
                fileName: item.file.name,
                fileSize: item.file.size
            });

            this._update(item);
            await this._pump(item);

        } catch (error) {
            this._fail(item, error);
        }
    }

    async _pump(item) {
        const totalChunks = Math.ceil(
            item.file.size / item.chunkSize
        );

        while (item.nextChunkIndex < totalChunks) {
            if (
                item.state === "paused" ||
                item.state === "cancelled"
            ) {
                return;
            }

            item.controller = new AbortController();

            const start =
                item.nextChunkIndex * item.chunkSize;

            const end = Math.min(
                start + item.chunkSize,
                item.file.size
            );

            const blob =
                item.file.slice(start, end);

            const form = new FormData();

            form.append(
                "file",
                blob,
                item.file.name
            );

            const headers = this._headers();

            if (this.verifyChunkChecksum) {
                headers["X-Chunk-SHA256"] =
                    await this._sha256(blob);
            }

            let response = null;

            for (let attempt = 1; attempt <= 3; attempt++) {
                try {
                    response = await this._json(
                        `${this.endpoint}/${item.uploadId}/chunks/${item.nextChunkIndex}`,
                        {
                            method: "POST",
                            headers,
                            body: form,
                            signal: item.controller.signal
                        }
                    );

                    break;

                } catch (error) {
                    if (error.name === "AbortError") {
                        throw error;
                    }

                    if (attempt === 3) {
                        throw error;
                    }

                    await new Promise(resolve =>
                        setTimeout(resolve, attempt * 1000)
                    );
                }
            }

            const serverNext =
                Number(response?.nextChunkIndex);

            const serverUploaded =
                Number(response?.uploadedSize);

            item.nextChunkIndex =
                Number.isInteger(serverNext)
                    ? serverNext
                    : item.nextChunkIndex + 1;

            item.uploadedBytes =
                Number.isFinite(serverUploaded)
                    ? serverUploaded
                    : end;

            this._progress(item);
            this._update(item);
        }

        const finalStatus = await this._json(
            `${this.endpoint}/${item.uploadId}/status`,
            {
                method: "GET",
                headers: this._headers()
            }
        );

        const receivedChunks =
            Number(finalStatus?.receivedChunks);

        const serverTotalChunks =
            Number(finalStatus?.totalChunks);

        const uploadedSize =
            Number(finalStatus?.uploadedSize);

        const totalSize =
            Number(finalStatus?.totalSize);

        if (
            !Number.isInteger(receivedChunks) ||
            !Number.isInteger(serverTotalChunks) ||
            receivedChunks !== serverTotalChunks ||
            uploadedSize !== totalSize
        ) {
            const serverNextIndex =
                Number(finalStatus?.nextChunkIndex);

            if (Number.isInteger(serverNextIndex)) {
                item.nextChunkIndex =
                    serverNextIndex;
            }

            if (Number.isFinite(uploadedSize)) {
                item.uploadedBytes =
                    uploadedSize;
            }

            throw new Error(
                `Upload is not complete on server ` +
                `(${receivedChunks || 0}/` +
                `${serverTotalChunks || totalChunks} chunks)`
            );
        }

        item.state = "finalizing";
        item.finalizingMessage =
            "Merging chunks and verifying file integrity...";

        item.speed = 0;
        item.eta = 0;

        this._update(item);

        const done = await this._json(
            `${this.endpoint}/${item.uploadId}/complete`,
            {
                method: "POST",
                headers: this._headers()
            }
        );

        item.state = "completed";
        item.finalizingMessage = "";
        item.uploadedBytes = item.file.size;
        item.error = "";

        localStorage.removeItem(
            "halo-upload:" + item.key
        );

        this._update(item);

        this._emit("upload-completed", {
            ...done,
            uploadId:
                done?.uploadId ||
                item.uploadId,
            originalFileName:
                done?.originalFileName ||
                item.file.name,
            fileName:
                done?.fileName ||
                item.file.name,
            fileSize:
                done?.fileSize ??
                item.file.size,
            mimeType:
                done?.mimeType ||
                item.file.type ||
                "application/octet-stream"
        });
    }

    pause(item) {
        if (item.state !== "uploading") {
            return;
        }

        item.state = "paused";
        item.controller?.abort();
        this._update(item);
    }

    async resume(item) {
        if (
            item.state !== "paused" &&
            item.state !== "failed"
        ) {
            return;
        }

        if (!item.uploadId) {
            await this.retry(item);
            return;
        }

        item.state = "uploading";
        item.error = "";

        this._update(item);

        try {
            const status = await this._json(
                `${this.endpoint}/${item.uploadId}/status`,
                {
                    method: "GET",
                    headers: this._headers()
                }
            );

            item.chunkSize = Number(
                status.chunkSize ||
                item.chunkSize ||
                this.requestedChunkSize
            );

            item.nextChunkIndex = Number(
                status.nextChunkIndex || 0
            );

            item.uploadedBytes = Number(
                status.uploadedSize || 0
            );

            item.startTime = performance.now();
            item.lastSampleTime = item.startTime;
            item.lastSampleBytes = item.uploadedBytes;

            this._update(item);
            await this._pump(item);

        } catch (error) {
            this._fail(item, error);
        }
    }

    async retry(item) {
        if (
            item.state !== "failed" &&
            item.state !== "cancelled"
        ) {
            return;
        }

        item.controller?.abort();

        item.state = "queued";
        item.error = "";
        item.speed = 0;
        item.eta = 0;

        this._update(item);

        if (item.uploadId) {
            try {
                const status = await this._json(
                    `${this.endpoint}/${item.uploadId}/status`,
                    {
                        method: "GET",
                        headers: this._headers()
                    }
                );

                item.chunkSize = Number(
                    status.chunkSize ||
                    this.requestedChunkSize
                );

                item.nextChunkIndex = Number(
                    status.nextChunkIndex || 0
                );

                item.uploadedBytes = Number(
                    status.uploadedSize || 0
                );

                item.state = "uploading";
                item.startTime = performance.now();
                item.lastSampleTime = item.startTime;
                item.lastSampleBytes =
                    item.uploadedBytes;

                this._update(item);
                await this._pump(item);
                return;

            } catch (error) {
                localStorage.removeItem(
                    "halo-upload:" + item.key
                );

                item.uploadId = null;
                item.nextChunkIndex = 0;
                item.uploadedBytes = 0;
            }
        }

        await this._start(item);
    }

    async cancel(item) {
        if (
            item.state === "completed" ||
            item.state === "finalizing" ||
            item.state === "cancelled"
        ) {
            return;
        }

        const previousState = item.state;

        item.state = "cancelled";
        item.controller?.abort();

        this._update(item);

        try {
            if (item.uploadId) {
                await fetch(
                    `${this.endpoint}/${item.uploadId}`,
                    {
                        method: "DELETE",
                        headers: this._headers(),
                        credentials: "same-origin"
                    }
                );
            }
        } catch (error) {
            console.warn(
                "Unable to cancel upload on server",
                error
            );
        } finally {
            localStorage.removeItem(
                "halo-upload:" + item.key
            );

            this._emit("upload-cancelled", {
                uploadId: item.uploadId || "",
                fileName: item.file.name,
                previousState
            });

            this._update(item);
        }
    }

    remove(item) {
        if (!item) {
            return;
        }

        if (
            item.state === "uploading" ||
            item.state === "initializing" ||
            item.state === "finalizing"
        ) {
            return;
        }

        item.controller?.abort();

        localStorage.removeItem(
            "halo-upload:" + item.key
        );

        if (item.card) {
            item.card.remove();
        }

        this.items.delete(item.key);
        this._refreshCompletedSection();
    }

    preview(item) {
        if (!item?.uploadId) {
            return;
        }

        const url =
            `${this.endpoint}/${item.uploadId}/preview`;

        window.open(
            url,
            "_blank",
            "noopener,noreferrer"
        );
    }

    download(item) {
        if (!item?.uploadId) {
            return;
        }

        const url =
            `${this.endpoint}/${item.uploadId}/download`;

        window.open(
            url,
            "_blank",
            "noopener,noreferrer"
        );
    }

    pauseAll() {
        this.items.forEach(item =>
            this.pause(item)
        );
    }

    resumeAll() {
        this.items.forEach(item => {
            if (
                item.state === "paused" ||
                item.state === "failed"
            ) {
                this.resume(item);
            }
        });
    }

    cancelAll() {
        this.items.forEach(item =>
            this.cancel(item)
        );
    }

    clearCompleted() {
        [...this.items.values()]
            .filter(item => item.state === "completed")
            .forEach(item => this.remove(item));

        this.completedExpanded = false;
        this._refreshCompletedSection();
    }

    toggleCompleted() {
        const completedCount = [...this.items.values()]
            .filter(item => item.state === "completed")
            .length;

        if (completedCount === 0) {
            return;
        }

        this.completedExpanded = !this.completedExpanded;
        this._refreshCompletedSection();
    }

    _refreshCompletedSection() {
        if (!this.completedSection) {
            return;
        }

        const completedCount = [...this.items.values()]
            .filter(item => item.state === "completed")
            .length;

        this.completedSection.classList.toggle(
            "visible",
            completedCount > 0
        );

        this.completedSection.classList.toggle(
            "expanded",
            completedCount > 0 && this.completedExpanded
        );

        this.completedHeader.setAttribute(
            "aria-expanded",
            String(completedCount > 0 && this.completedExpanded)
        );

        this.completedLabel.textContent =
            `Completed (${completedCount})`;

        if (completedCount === 0) {
            this.completedExpanded = false;
        }
    }

    _moveCardToCorrectList(item) {
        if (!item.card) {
            return;
        }

        const target = item.state === "completed"
            ? this.completedList
            : this.list;

        if (item.card.parentElement !== target) {
            target.appendChild(item.card);
        }
    }

    _render(item) {
        const card =
            document.createElement("div");

        card.className = "card";

        card.innerHTML = `
            <div class="row file-header">
                <div class="file-icon">📄</div>
                <div class="name"></div>
                <div class="meta size"></div>
            </div>

            <progress max="100" value="0"></progress>

            <div class="row status-row">
                <div class="meta status"></div>
                <div class="meta stats"></div>
            </div>

            <div class="actions"></div>
            <div class="error"></div>
        `;

        card.querySelector(".name").textContent =
            item.file.name;

        card.querySelector(".name").title =
            item.file.name;

        card.querySelector(".size").textContent =
            this._size(item.file.size);

        card.querySelector(".file-icon").textContent =
            this._fileIcon(item.file);

        item.card = card;

        this.list.appendChild(card);
        this._update(item);
    }

    _update(item) {
        if (!item.card) {
            return;
        }

        const percentage = item.file.size
            ? item.uploadedBytes * 100 / item.file.size
            : 0;

        const safePercentage = Math.max(
            0,
            Math.min(100, percentage)
        );

        const progress =
            item.card.querySelector("progress");

        const statusElement =
            item.card.querySelector(".status");

        const statsElement =
            item.card.querySelector(".stats");

        const errorElement =
            item.card.querySelector(".error");

        progress.value = safePercentage;

        item.card.classList.remove(
            "completed",
            "failed",
            "paused"
        );

        statusElement.className =
            "meta status";

        if (item.state === "queued") {
            statusElement.textContent =
                "Waiting to upload...";

            statsElement.textContent =
                this._size(item.file.size);

        } else if (item.state === "initializing") {
            statusElement.textContent =
                "Preparing upload...";

            statsElement.textContent =
                this._size(item.file.size);

        } else if (item.state === "uploading") {
            statusElement.textContent =
                "Uploading...";

            statsElement.textContent =
                `${safePercentage.toFixed(1)}% · ` +
                `${this._size(item.uploadedBytes)} / ` +
                `${this._size(item.file.size)}` +
                (
                    item.speed
                        ? ` · ${this._size(item.speed)}/s` +
                          ` · ETA ${this._duration(item.eta)}`
                        : ""
                );

        } else if (item.state === "paused") {
            item.card.classList.add("paused");

            statusElement.textContent =
                "Upload paused";

            statusElement.className =
                "meta status warning";

            statsElement.textContent =
                `${safePercentage.toFixed(1)}% · ` +
                `${this._size(item.uploadedBytes)} / ` +
                `${this._size(item.file.size)}`;

        } else if (item.state === "finalizing") {
            statusElement.textContent =
                "Finalizing upload...";

            statusElement.className =
                "meta status finalizing";

            statsElement.textContent =
                item.finalizingMessage ||
                "Merging chunks and verifying file integrity...";

            progress.value = 100;

        } else if (item.state === "completed") {
            item.card.classList.add("completed");

            statusElement.textContent =
                "Completed";

            statusElement.className =
                "meta status success";

            statsElement.textContent =
                `100.0% · ` +
                `${this._size(item.file.size)} / ` +
                `${this._size(item.file.size)}`;

            progress.value = 100;

        } else if (item.state === "failed") {
            item.card.classList.add("failed");

            statusElement.textContent =
                "Upload failed";

            statsElement.textContent =
                `${safePercentage.toFixed(1)}% · ` +
                `${this._size(item.uploadedBytes)} / ` +
                `${this._size(item.file.size)}`;

        } else if (item.state === "cancelled") {
            statusElement.textContent =
                "Upload cancelled";

            statsElement.textContent =
                `${safePercentage.toFixed(1)}%`;
        } else {
            statusElement.textContent =
                item.state || "Unknown";
        }

        errorElement.textContent =
            item.error || "";

        this._renderActions(item);
        this._moveCardToCorrectList(item);
        this._refreshCompletedSection();
    }

    _renderActions(item) {
        const actions =
            item.card.querySelector(".actions");

        actions.replaceChildren();

        switch (item.state) {
            case "queued":
            case "initializing":
                this._addActionButton(
                    actions,
                    "✖",
                    "Cancel",
                    () => this.cancel(item),
                    "danger"
                );
                break;

            case "uploading":
                this._addActionButton(
                    actions,
                    "⏸",
                    "Pause",
                    () => this.pause(item)
                );

                this._addActionButton(
                    actions,
                    "✖",
                    "Cancel",
                    () => this.cancel(item),
                    "danger"
                );
                break;

            case "paused":
                this._addActionButton(
                    actions,
                    "▶",
                    "Resume",
                    () => this.resume(item),
                    "primary"
                );

                this._addActionButton(
                    actions,
                    "✖",
                    "Cancel",
                    () => this.cancel(item),
                    "danger"
                );
                break;

            case "failed":
                this._addActionButton(
                    actions,
                    "↻",
                    "Retry",
                    () => this.retry(item),
                    "primary"
                );

                this._addActionButton(
                    actions,
                    "🗑",
                    "Remove",
                    () => this.remove(item),
                    "danger"
                );
                break;

            case "completed":
                this._addActionButton(
                    actions,
                    "👁",
                    "Preview",
                    () => this.preview(item)
                );

                this._addActionButton(
                    actions,
                    "⬇",
                    "Download",
                    () => this.download(item),
                    "primary"
                );

                this._addActionButton(
                    actions,
                    "✕",
                    "Remove",
                    () => this.remove(item)
                );
                break;

            case "cancelled":
                this._addActionButton(
                    actions,
                    "↻",
                    "Retry",
                    () => this.retry(item),
                    "primary"
                );

                this._addActionButton(
                    actions,
                    "✕",
                    "Remove",
                    () => this.remove(item)
                );
                break;

            case "finalizing":
                // No buttons while the server is merging
                // and verifying the uploaded file.
                break;

            default:
                break;
        }
    }

    _addActionButton(
        container,
        icon,
        caption,
        handler,
        className = ""
    ) {
        const button =
            document.createElement("button");

        button.type = "button";
        button.title = caption;
        button.setAttribute(
            "aria-label",
            caption
        );

        if (className) {
            button.classList.add(className);
        }

        const iconElement =
            document.createElement("span");

        iconElement.textContent = icon;

        const captionElement =
            document.createElement("span");

        captionElement.textContent = caption;

        button.append(
            iconElement,
            captionElement
        );

        button.onclick = event => {
            event.preventDefault();
            event.stopPropagation();
            handler();
        };

        container.appendChild(button);
    }

    _progress(item) {
        const now = performance.now();

        const elapsedSeconds =
            (now - item.lastSampleTime) / 1000;

        const uploadedSinceLastSample =
            item.uploadedBytes -
            item.lastSampleBytes;

        if (elapsedSeconds > 0.2) {
            item.speed =
                uploadedSinceLastSample /
                elapsedSeconds;

            item.eta =
                item.speed > 0
                    ? (
                        item.file.size -
                        item.uploadedBytes
                    ) / item.speed
                    : 0;

            item.lastSampleTime = now;
            item.lastSampleBytes =
                item.uploadedBytes;
        }

        this._emit("upload-progress", {
            uploadId: item.uploadId,
            fileName: item.file.name,
            uploadedBytes: item.uploadedBytes,
            totalBytes: item.file.size,
            progressPercent:
                item.file.size > 0
                    ? (
                        item.uploadedBytes *
                        100 /
                        item.file.size
                    )
                    : 0,
            speedBytesPerSecond:
                item.speed || 0,
            etaSeconds:
                item.eta || 0
        });
    }

    _fail(item, error) {
        if (
            item.state === "paused" ||
            item.state === "cancelled" ||
            error?.name === "AbortError"
        ) {
            return;
        }

        item.state = "failed";
        item.error =
            error?.message ||
            "Upload failed";

        this._update(item);

        this._emit("upload-failed", {
            uploadId: item.uploadId || "",
            fileName: item.file.name,
            message: item.error
        });
    }

    async _json(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options
        });

        if (!response.ok) {
            let message =
                `HTTP ${response.status}`;

            try {
                const result =
                    await response.json();

                message =
                    result.message ||
                    result.error ||
                    message;

            } catch (error) {
                try {
                    const text =
                        await response.text();

                    if (text) {
                        message = text;
                    }
                } catch (ignored) {
                    // Keep the HTTP status message.
                }
            }

            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        const contentType =
            response.headers.get("content-type") || "";

        if (
            contentType.includes(
                "application/json"
            )
        ) {
            return response.json();
        }

        return null;
    }

    _headers(extra = {}) {
        const headers = { ...extra };

        const csrfToken =
            document.querySelector(
                'meta[name="_csrf"]'
            );

        const csrfHeader =
            document.querySelector(
                'meta[name="_csrf_header"]'
            );

        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] =
                csrfToken.content;
        }

        const xsrfCookie =
            document.cookie
                .split("; ")
                .find(value =>
                    value.startsWith(
                        "XSRF-TOKEN="
                    )
                );

        if (
            xsrfCookie &&
            !headers["X-XSRF-TOKEN"]
        ) {
            headers["X-XSRF-TOKEN"] =
                decodeURIComponent(
                    xsrfCookie
                        .split("=")
                        .slice(1)
                        .join("=")
                );
        }

        return headers;
    }

    async _sha256(blob) {
        const buffer =
            await blob.arrayBuffer();

        const hash =
            await crypto.subtle.digest(
                "SHA-256",
                buffer
            );

        return [...new Uint8Array(hash)]
            .map(value =>
                value
                    .toString(16)
                    .padStart(2, "0")
            )
            .join("");
    }

    _fingerprint(file) {
        return [
            file.name,
            file.size,
            file.lastModified
        ].join(":");
    }

    _fileIcon(file) {
        const name =
            file.name.toLowerCase();

        const type =
            (file.type || "")
                .toLowerCase();

        if (
            type === "application/pdf" ||
            name.endsWith(".pdf")
        ) {
            return "📕";
        }

        if (type.startsWith("image/")) {
            return "🖼️";
        }

        if (type.startsWith("video/")) {
            return "🎬";
        }

        if (type.startsWith("audio/")) {
            return "🎵";
        }

        if (
            name.endsWith(".xls") ||
            name.endsWith(".xlsx") ||
            name.endsWith(".csv")
        ) {
            return "📊";
        }

        if (
            name.endsWith(".doc") ||
            name.endsWith(".docx")
        ) {
            return "📘";
        }

        if (
            name.endsWith(".ppt") ||
            name.endsWith(".pptx")
        ) {
            return "📙";
        }

        if (
            name.endsWith(".zip") ||
            name.endsWith(".rar") ||
            name.endsWith(".7z")
        ) {
            return "🗜️";
        }

        if (name.endsWith(".iso")) {
            return "💿";
        }

        return "📄";
    }

    _emit(name, detail) {
        this.dispatchEvent(
            new CustomEvent(name, {
                detail,
                bubbles: true,
                composed: true
            })
        );
    }

    _size(bytes) {
        if (
            !Number.isFinite(bytes) ||
            bytes <= 0
        ) {
            return "0 B";
        }

        const units = [
            "B",
            "KB",
            "MB",
            "GB",
            "TB"
        ];

        let value = bytes;
        let unitIndex = 0;

        while (
            value >= 1024 &&
            unitIndex < units.length - 1
        ) {
            value /= 1024;
            unitIndex++;
        }

        return (
            value.toFixed(unitIndex ? 1 : 0) +
            " " +
            units[unitIndex]
        );
    }

    _duration(seconds) {
        const safeSeconds = Math.max(
            0,
            Math.round(seconds || 0)
        );

        const hours =
            Math.floor(safeSeconds / 3600);

        const minutes =
            Math.floor(
                (safeSeconds % 3600) / 60
            );

        const remainingSeconds =
            safeSeconds % 60;

        if (hours > 0) {
            return (
                `${hours}h ${minutes}m ` +
                `${remainingSeconds}s`
            );
        }

        if (minutes > 0) {
            return (
                `${minutes}m ` +
                `${remainingSeconds}s`
            );
        }

        return `${remainingSeconds}s`;
    }
}

if (
    !customElements.get(
        "halo-large-file-uploader"
    )
) {
    customElements.define(
        "halo-large-file-uploader",
        HaloLargeFileUploader
    );
}