// ==UserScript==
// @name         Cardmarket → PostNord Label
// @namespace    http://tampermonkey.net/
// @version      1.8
// @updateURL    https://raw.githubusercontent.com/nilssonjn/postnord-cm-shipping/main/cardmarket-postnord-label.user.js
// @downloadURL  https://raw.githubusercontent.com/nilssonjn/postnord-cm-shipping/main/cardmarket-postnord-label.user.js
// @match        https://www.cardmarket.com/*/Orders/*
// @grant        GM_xmlhttpRequest
// @connect      localhost
// ==/UserScript==
(function () {
    'use strict';
    const BACKEND_URL = 'http://localhost:8081/api/labels/generate';
    const DEFAULT_WEIGHT_GRAMS = 50; // fallback only, used if Cardmarket doesn't render a "(max. Ng)" hint
    const BUTTON_ID = 'postnord-label-btn';
    // Postal code format per shipping country, matched against the combined
    // "<zip> <city>" string Cardmarket renders in a single DOM node.
    const ZIP_PATTERNS = {
        'Sweden': /^(\d{3}\s?\d{2})\s+(.+)$/,
        'Portugal': /^(\d{4}-\d{3})\s+(.+)$/,
        'Poland': /^(\d{2}-\d{3})\s+(.+)$/,
        'Netherlands': /^(\d{4}\s?[A-Za-z]{2})\s+(.+)$/,
        'Ireland': /^([A-Za-z][A-Za-z0-9]{2}\s?[A-Za-z0-9]{4})\s+(.+)$/,
        'Latvia': /^(LV-?\d{4})\s+(.+)$/i,
        'Lithuania': /^(LT-?\d{5})\s+(.+)$/i,
        'Estonia': /^(\d{5})\s+(.+)$/,
    };
    // Covers most European formats not listed above: an optional 1-2 letter
    // prefix, optional dash, 3-5 digits, optional 1-2 letter suffix, with no
    // internal space (countries whose code contains a space, e.g. Sweden,
    // Netherlands, need their own explicit entry above).
    const DEFAULT_ZIP_PATTERN = /^([A-Za-z]{0,2}-?\d{3,5}[A-Za-z]{0,2})\s+(.+)$/;

    function extractAddress() {
        const name = document.querySelector('#ShippingAddress .Name')?.innerText.trim();
        const street2 = document.querySelector('#ShippingAddress .Extra')?.innerText.trim() || null;
        const street = document.querySelector('#ShippingAddress .Street')?.innerText.trim();
        const rawCity = document.querySelector('#ShippingAddress .City')?.innerText.trim();
        const country = document.querySelector('#ShippingAddress .Country')?.innerText.trim();
        if (!name || !street || !rawCity || !country) {
            throw new Error('Missing name/street/city/country on page. Is this an order page?');
        }
        const m = rawCity.match(ZIP_PATTERNS[country] ?? DEFAULT_ZIP_PATTERN);
        if (!m) {
            console.warn(`[postnord-label] No postal code pattern matched for country "${country}", city field "${rawCity}". Add an entry to ZIP_PATTERNS or widen DEFAULT_ZIP_PATTERN.`);
            throw new Error(`Could not parse postal code/city from "${rawCity}" (country: ${country}). See browser console for details.`);
        }
        const zip = m[1];
        const city = m[2];
        const shippingDd = Array.from(document.querySelectorAll('dt'))
            .find(el => el.textContent.trim() === 'Shipping Method:')
            ?.nextElementSibling;
        const rawService = shippingDd
            ?.querySelector('span:not([data-bs-toggle]):not(.ms-1)')
            ?.textContent?.trim() ?? '';
        // The "(max. Ng)" hint lives in its own sibling span (class "ms-1 text-muted"),
        // not inside the service-name span above.
        const rawWeightText = shippingDd?.querySelector('span.ms-1')?.textContent?.trim() ?? '';
        const maxWeightMatch = rawWeightText.match(/\(max\.\s*(\d+)g\)/);
        const serviceType = rawService.replace(/\s*\(max\.\s*\d+g\)/, '').trim();
        const weightGrams = maxWeightMatch ? parseInt(maxWeightMatch[1], 10) : DEFAULT_WEIGHT_GRAMS;
        const orderId = window.location.pathname.match(/\/Orders\/(\d+)/)?.[1] ?? 'unknown';
        const phoneDt = Array.from(document.querySelectorAll('dt'))
            .find(el => el.textContent.trim() === 'Phone Number:');
        const rawPhone = phoneDt?.nextElementSibling?.textContent?.trim() ?? null;
        const buyerPhone = rawPhone ? rawPhone.replace(/^00(\d+)-0?/, '+$1') : null;
        const emailDt = Array.from(document.querySelectorAll('dt'))
            .find(el => el.textContent.trim() === 'Email:');
        const buyerEmail = emailDt?.nextElementSibling
            ?.querySelector('span:not([data-bs-toggle])')
            ?.textContent?.trim() || null;
        return {
            buyerName: name, street, street2, postalCode: zip, city,
            countryName: country, orderId, serviceType,
            weightGrams, buyerPhone, buyerEmail
        };
    }

    function setButtonState(btn, state) {
        const states = {
            idle: {text: 'Generate Label', color: '#1a73e8', disabled: false},
            loading: {text: 'Booking...', color: '#888', disabled: true},
            success: {text: 'Downloaded ✓', color: '#2e7d32', disabled: true},
            error: {text: 'Failed ✗', color: '#c62828', disabled: false},
        };
        const s = states[state];
        btn.innerText = s.text;
        btn.style.background = s.color;
        btn.disabled = s.disabled;
    }

    function injectButton() {
        const addressBlock = document.querySelector('#ShippingAddress');
        if (!addressBlock || document.getElementById(BUTTON_ID)) return;
        const btn = document.createElement('button');
        btn.id = BUTTON_ID;
        btn.innerText = 'Generate Label';
        btn.style.cssText = `
            margin-top: 10px;
            padding: 8px 14px;
            background: #1a73e8;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 13px;
            display: block;
        `;
        btn.onclick = () => {
            let data;
            try {
                data = extractAddress();
            } catch (err) {
                console.error('[postnord-label]', err);
                alert(err.message);
                return;
            }
            setButtonState(btn, 'loading');
            GM_xmlhttpRequest({
                method: 'POST',
                url: BACKEND_URL,
                headers: {'Content-Type': 'application/json'},
                data: JSON.stringify(data),
                responseType: 'blob',
                onload: (res) => {
                    if (res.status === 200) {
                        const blob = new Blob([res.response], {type: 'application/zpl'});
                        const link = document.createElement('a');
                        link.href = URL.createObjectURL(blob);
                        link.download = `label-${data.orderId}.zpl`;
                        link.click();
                        setButtonState(btn, 'success');
                        setTimeout(() => setButtonState(btn, 'idle'), 3000);
                    } else {
                        console.error('PostNord label error:', res.status, res.responseText);
                        setButtonState(btn, 'error');
                        alert(`Label generation failed (${res.status}):\n${res.responseText}`);
                    }
                },
                onerror: (err) => {
                    console.error('Backend unreachable:', err);
                    setButtonState(btn, 'error');
                    alert('Could not reach the backend. Is the app running?');
                }
            });
        };
        addressBlock.appendChild(btn);
    }

    injectButton();
    const observer = new MutationObserver(injectButton);
    observer.observe(document.body, {childList: true, subtree: true});
})();
