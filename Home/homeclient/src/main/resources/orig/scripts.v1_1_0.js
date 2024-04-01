setAutoRefresh();
registerOnFocusAutoRefresh();
$(document).ready(function(){
    setAppTitle(null);
    registerCollapseListener();
    registerButtonListener();
    showUserErrorMessage();
    doOnloadFunctions();
});

function registerOnFocusAutoRefresh(){
    let element = document.getElementById('idbody');
    if (element !== null && element.value !== '' && !isApp()) {
        document.getElementById('idbody').onfocus = function(){
            doAutoRefresh();
            setAutoRefresh();
        };
        document.getElementById('idbody').onblur = function(){
            unsetAutoRefresh();
            showStop();
        };
    }
}

function setPushToken(newTokenValue, newClientName){
    clientName = newClientName;
    let oldValue = pushToken;
    pushToken = newTokenValue;
    if(newTokenValue !== oldValue){
        refreshContent();
    }
}

function showUserErrorMessage(){
    if(document.getElementById('idUserErrorMessage')){
        $('#idUserErrorMessage').modal('show');
    }
}

function submitContentWithPin(target, pin){
    if(navigator.onLine){
        let etag = "";
        if(document.getElementById('modelTimestamp')){
            etag = document.getElementById('modelTimestamp').value;
        }
        let httpRequest = new XMLHttpRequest();
        httpRequest.open('GET', target);
        httpRequest.setRequestHeader('Cache-Control', 'no-cache');
        httpRequest.setRequestHeader('ETag', etag);
        httpRequest.setRequestHeader('clientName', clientName);
        httpRequest.setRequestHeader('isAjaxRequest', 'true');
        httpRequest.setRequestHeader('CSRF', 'true');
        httpRequest.setRequestHeader('appPushToken', pushToken);
        httpRequest.setRequestHeader('pin', pin.replace(/[^a-zA-Z0-9]/g, ''));
        httpRequest.send();
        httpRequest.onreadystatechange=(e)=>{
            if (httpRequest.readyState === 4) {
                if(target===window.location.href && !isViewStatePermittingRefresh()){
                    // do not replace content while gui transition is running
                    showCheck();
                }else if(httpRequest.status === 200){
                    let doc = new DOMParser().parseFromString(httpRequest.responseText, "text/html");
                    preserveStatus(document, doc);
                    document.getElementById('idbody').innerHTML = doc.getElementById('idbody').innerHTML;
                    lastRefresh = (new Date).getTime();
                    registerCollapseListener();
                    registerButtonListener();
                    showCheck();
                    showUserErrorMessage();
                    doOnloadFunctions();
                } else if(httpRequest.status === 304){
                    lastRefresh = (new Date).getTime();
                    showCheck();
                    if(isFirstRefreshAfterAppBackgroundPending){
                        isFirstRefreshAfterAppBackgroundPending = false;
                        nativeMessage("loadedView");
                    }
                }else{
                    showOffline();
                }
                setAppTitle(httpRequest);
            }
        }
    }else{
        showOffline();
    }
}

function doOnloadFunctions(){
    if(sliderList){
        prototypeSlider();
        let sliderKey;
        for (sliderKey in sliderList) {
            initSliderFunctionsByName[sliderList[sliderKey]](sliderKey);
        }
    }
    if(rangeIDs){
        rangeIDs.forEach(initRangecontainer);
    }
    nativeMessage("loadedView");
}

function initRangecontainer(id, index, array){
    if(document.getElementById("rangecontainer-div-" + id)){ // nicht ausgeblendet
        let consumption = document.getElementById("rangecontainer-consumption-" + id).value;
        let maxGrid = document.getElementById("rangecontainer-rangemaxgridvalue-" + id).value;
        document.getElementById("rangecontainer-rangeinput-" + id).value = 100 - Math.round(maxGrid * 100 / consumption);
        document.getElementById("rangecontainer-rangeinput-" + id).oninput = function() {
            rangelabels(id);
        }
        document.getElementById("rangecontainer-rangeinput-" + id).onmousedown = function() {
            $('#rangecontainer-div-' + id).addClass('doNotRefresh');
        }
        document.getElementById("rangecontainer-rangeinput-" + id).onmouseup = function() {
            $('#rangecontainer-div-' + id).removeClass('doNotRefresh');
            rangelabels(id);
            let link = document.getElementById("rangecontainer-rangemaxgridvalue-link-" + id).value;
            let value = document.getElementById("rangecontainer-rangemaxgridvalue-" + id).value;
            console.log("SUBMIT " + link + value);
            submitContent(link + value);
        }
        rangelabels(id);
    }
}

function rangelabels(id) {
    let consumption = document.getElementById("rangecontainer-consumption-" + id).value;
    let percentage = document.getElementById("rangecontainer-rangeinput-" + id).value;
    let minPV = Math.round(consumption * percentage / 100);
    let maxGrid = Math.round(consumption - (consumption * percentage / 100));
    document.getElementById("rangecontainer-label-2-" + id).innerHTML = percentage + "%";
    document.getElementById("rangecontainer-label-1-" + id).innerHTML = (minPV === 0 ? "" : "min ") + minPV + "W PV";
    document.getElementById("rangecontainer-label-3-" + id).innerHTML = (maxGrid === 0 ? "" : "max ") + maxGrid + "W Netz";
    document.getElementById("rangecontainer-rangemaxgridvalue-" + id).value = maxGrid;
}

function submitContent(target){
    submitContentWithPin(target, '');
}

function preserveStatus(oldDoc, newDoc){
    for (let entry of oldDoc.getElementsByClassName('collapse-preserve-state')){
        if(entry.classList.contains('show')){
            openCollapsable(newDoc, entry.id);
        }
    }
    for (let entry of oldDoc.getElementsByClassName('checkbox-preserve-state')){
        if(entry.checked){
            if(newDoc.getElementById(entry.id)!==null && newDoc.getElementById(entry.id).checked === false){
                newDoc.getElementById(entry.id).setAttribute("checked", "checked");
            }
        }
    }
}

function openCollapsable(document, id){
    if(document.getElementById(id)!==null){
        document.getElementById(id).classList.remove("collapse");
        document.getElementById(id).classList.add("show");
        if(document.getElementById(id + 'ElementTitleState')){
            document.getElementById(id + 'ElementTitleState').innerHTML = '';
        }
        document.getElementById(id + 'SymbolDown').style.display = 'none';
        document.getElementById(id + 'SymbolUp').style.display = 'inline';
    }
}

function registerCollapseListener(){
    // start transitions
    let collapsePreserveState = $('.collapse-preserve-state');
    collapsePreserveState.on('show.bs.collapse', function (event) {
        if(document.getElementById(event.target.id + 'SymbolDown') !== null){
            document.getElementById(event.target.id + 'SymbolDown').style.display = 'none';
            document.getElementById(event.target.id + 'SymbolUp').style.display = 'inline';
            nativeMessage('startTransition')
        }
    });
    collapsePreserveState.on('hide.bs.collapse', function (event) {
        if(document.getElementById(event.target.id + 'SymbolDown') !== null){
            document.getElementById(event.target.id + 'SymbolDown').style.display = 'inline';
            document.getElementById(event.target.id + 'SymbolUp').style.display = 'none';
            nativeMessage('startTransition')
        }
    });
    // end transitions
    collapsePreserveState.on('shown.bs.collapse', function (event) {
        if(document.getElementById(event.target.id + 'ElementTitleState') !== null){
            document.getElementById(event.target.id + 'ElementTitleState').innerHTML = '';
            nativeMessage('endTransition')
        }
    });
    collapsePreserveState.on('hidden.bs.collapse', function (event) {
        if(document.getElementById(event.target.id + 'ElementTitleState') !== null){
            document.getElementById(event.target.id + 'ElementTitleState').innerHTML = document.getElementById('val_ElementTitleState_' + event.target.id).value;
            nativeMessage('endTransition')
        }
    });
}

function registerButtonListener(){
    $('.btn').on('click', function(event) {
        nativeMessage('startButtonPress')
    });
}

function nativeMessage(message){
    try {
        if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers['homeMessageHandler']) {
            window.webkit.messageHandlers['homeMessageHandler'].postMessage(message);
        }
    } catch(err) {
        console.log('error sending nativeMessage: ' + err);
    }
}

function refreshContent(){
    submitContent(window.location.href);
}

function showCheck(){
    if (document.getElementById('statusline') !== null ) {
        document.getElementById('navbarcheck').style.display = 'inline';
        document.getElementById('navbaroffline').style.display = 'none';
        document.getElementById('navbarstop').style.display = 'none';
    }
}
function showOffline(){
    if (document.getElementById('statusline') !== null ) {
        document.getElementById('navbarcheck').style.display = 'none';
        document.getElementById('navbaroffline').style.display = 'inline';
        document.getElementById('navbarstop').style.display = 'none';
    }
}
function showStop(){
    if (document.getElementById('statusline') !== null ) {
        document.getElementById('navbarcheck').style.display = 'none';
        document.getElementById('navbaroffline').style.display = 'none';
        document.getElementById('navbarstop').style.display = 'inline';
    }
}

function isApp(){
    return document.getElementById('isApp') !== null && document.getElementById('isApp').value === 'true';
}

function setAppTitle(httpRequest){

    let ts = null;
    if(httpRequest != null && httpRequest.getResponseHeader('SITE_REQUEST_TS')!=null){
        ts = httpRequest.getResponseHeader('SITE_REQUEST_TS');
    }else{
        if(document.getElementById('SITE_REQUEST_TS') != null){
            ts = document.getElementById('SITE_REQUEST_TS').value;
        }
    }

    if(ts != null){
        if(isApp()){
            if(ts !== modelTimestampLastHandled){
                modelTimestampLastHandled = ts;
                document.title = "ts=" + ts;
                nativeMessage("modelTimestamp=" + ts);
            }
        }else{
            document.getElementById('idHeaderStatusText').innerHTML = ts;
            document.title = 'Zuhause - ' + ts;
        }
    }else if (document.getElementById('browserTitle') !== null){
        document.title = document.getElementById('browserTitle').value;
    }
}

function isAppInForeground(){
    return isApp() && document.getElementById('appInForegroundMarker').value === 'true';
}

function fastLinkTo(id){
    $('#fastLinkMarker').addClass('doNotRefresh');
    try{
        let cleanedId = id.replace('_groupitem2', '');
        cleanedId = cleanedId.replace('_groupitem', '');
        let divId = ('collapse' + cleanedId);
        let elementById = document.getElementById(divId);
        if(!elementById){
            throw('id not found:' + divId);
        }
        // open collapsable
        openCollapsable(document, divId);
        // open collapse of viewCorrelation
        let viewCorrelationId = 'viewCorrelation_' + cleanedId;
        let viewCorrelationElement = document.getElementById(viewCorrelationId);
        if(viewCorrelationElement){
            let viewCorrelationCollapsableId = 'collapse' + viewCorrelationElement.value;
            openCollapsable(document, viewCorrelationCollapsableId);
        }
        // scroll to previous title
        let parentCounter = 0;
        while((elementById && !elementById.classList.contains('alert') || !elementById.classList.contains('callout')) && parentCounter < 100){
            parentCounter++;
            elementById = elementById.parentElement;
        }
        if(elementById && elementById.classList.contains('alert') && elementById.classList.contains('callout')){
            let siblingCounter = 0;
            while(elementById && !elementById.classList.contains('placeTitle') && siblingCounter < 100){
                siblingCounter++;
                elementById = elementById.previousElementSibling;
            }
            if(elementById && elementById.classList.contains('placeTitle') ){
                elementById.scrollIntoView();
                let navbarHeight = 0;
                if(document.getElementById('navbar')){
                    navbarHeight = document.getElementById('navbar').offsetHeight;
                }
                window.scrollBy(0, (navbarHeight + 6) * -1);
            }
        }
    }finally{
        $('#fastLinkMarker').removeClass('doNotRefresh');
    }
}

// PIN start

function openPin(label, cb){
    pinCallback = cb;
    document.getElementById('modalPinLabelId').innerHTML = label();
    document.getElementById('val_pin').value = '';
    pinPlaceholderStars(document.getElementById('val_pin').value);
    document.getElementById('pin-biometry').style.display = 'none';
    nativeMessage('checkBiometricAuthAvailableForPin=pinActivateBiometricAuthButton');
    $("#modalPin").modal();
}
function resetPin(){
    document.getElementById('val_pin').value = '';
    pinPlaceholderStars('');
}
function addPin(i){
    document.getElementById('val_pin').value += i;
    pinPlaceholderStars(document.getElementById('val_pin').value);
    if(document.getElementById('val_pin').value.length === 6){
        pinCallback();
        $("#modalPin").modal('hide');
    }
}
function pinActivateBiometricAuthButton(){ // called from app
    document.getElementById('pin-biometry').style.display = 'block';
}
function pinBiometryStart(){
    nativeMessage('biometryGetPin=pinBiometryCallback');
}
function pinBiometryCallback(pin){
    document.getElementById('val_pin').value = pin;
    pinPlaceholderStars(pin);
    if(pin.length > 0){
        pinCallback();
        $("#modalPin").modal('hide');
    }
}
function pinPlaceholderStars(x){
    let p = '';
    for(let i = 1; i<=6; i++){
        if(i<=x.length){
            p += '* '
        }else{
            p += '_ ';
        }
    }
    document.getElementById('modalPinStarId').innerHTML = p;
}

// PIN end

function setAutoRefresh(){
    unsetAutoRefresh();
    autoRefresh = setTimeout(doAutoRefresh, 1000 * 2);
}
function unsetAutoRefresh(){
    if(autoRefresh !== null){
        clearTimeout(autoRefresh);
        autoRefresh = null;
    }
}
function doAutoRefresh(){

    // focus handling in app: app webview only gets focus after first touch event
    if((document.hasFocus() || isAppInForeground()) &&
        $('.modal.fade.show').length === 0 && ((new Date).getTime() - lastRefresh > (1000)) && isViewStatePermittingRefresh()){
        refreshContent();
    }
    if(document.hasFocus() || isAppInForeground()){
        setAutoRefresh();
    }
}

function isViewStatePermittingRefresh(){
    let transitions = document.getElementsByClassName('collapsing');
    return transitions.length === 0 && document.getElementsByClassName('doNotRefresh').length === 0;
}

function setAppInForegroundMarker(val){
    if(val===true){
        document.getElementById('appInForegroundMarker').value = 'true';
        isFirstRefreshAfterAppBackgroundPending = true;
        doAutoRefresh();
        setAutoRefresh();
    }else if(val===false){
        document.getElementById('appInForegroundMarker').value = 'false';
        unsetAutoRefresh();
        showStop();
    }
}

function openHistory(key){
    $("#modalHistory" + key).modal();
    var httpRequest = new XMLHttpRequest();
    httpRequest.open("GET", "/history?key=" + key);
    httpRequest.setRequestHeader('Cache-Control', 'no-cache');
    httpRequest.setRequestHeader('isAjaxRequest', 'true');
    httpRequest.send();
    httpRequest.onreadystatechange=(e)=>{
        if (httpRequest.readyState === 4 && httpRequest.status === 200) {
            document.getElementById("modalHistoryBody" + key).innerHTML = httpRequest.responseText;
        }
    }
}

function heatpump(id, place, link){
    if(!link.startsWith('#')){
        let linkComplete = link;
        for (let addPlace of document.getElementsByName('heatpumpAddPlaceFor_' + id)){
            if(addPlace.checked){
                linkComplete += addPlace.getAttribute('alt') + ",";
            }
        }
        submitContent(linkComplete);
    }
}

function prototypeSlider(){
    $.fn.roundSlider.prototype._invertRange = true;
    $.fn.roundSlider.prototype.defaults.create = function() {
        // numbers
        if(this.options.showMarkers){
            for (let i = this.options.min; i <= this.options.max; i += 25) {
                let angle = this._valueToAngle(i);
                let text = 100-i==100?"1/1":(i==75?"1/4":(i==50?"1/2":(i==25?"3/4":"0")));
                this._addSeperator(angle, "slider-separator").children().removeClass().addClass("rs-marker").html(text).rsRotate(-angle);
            }
        }
        // 1/8 ticks
        if(this.options.show8ticks){
            for (let i = this.options.min + 12.5; i <= this.options.max - 12.5; i += 12.5) {
                let angle = this._valueToAngle(i);
                this._addSeperator(angle, "slider-separator").children().removeClass().addClass("rs-tick8").html("&#8226;").rsRotate(-angle);
            }
        }
        // 1/16 ticks
        if(this.options.show16ticks){
            for (let i = this.options.min; i <= this.options.max; i += 6.25) {
                if(i%12.5!=0){
                    let angle = this._valueToAngle(i);
                    this._addSeperator(angle, "slider-separator").children().removeClass().addClass("rs-tick16").html("&#8226;").rsRotate(-angle);
                }
            }
        }
    }
}

function initEvChargeSlider(id){
    let startValue = 100 - document.getElementById(id + '-startValue').value;
    $('#' + id).roundSlider({
        editableTooltip: false,
        width: 12,
        radius: 160,
        handleShape: "square",
        handleSize: 1,
        value: startValue,
        svgMode: true,
        min: 0,
        max: 100,
        circleShape: "custom",
        startAngle: "75",
        endAngle: "+195",
        pathColor: "white",
        animation: false,
        showMarkers: true,
        show8ticks: true,
        show16ticks: true,
        step: "1",
        tooltipFormat: function (e) {
            return "";
        },
        beforeCreate: function (){
            document.getElementById(id + '-label').innerHTML = (100 - startValue) + "%";
            if(document.getElementById(id + "-isActual").value == "true"){
                document.getElementById(id + '-status').style.color = "green";
            }else{
                document.getElementById(id + '-status').style.color = "yellow";
            }
        },
        start: function (){
            $('#' + id).addClass('doNotRefresh');
            document.getElementById(id + "-eventState").value = 'drag';
        },
        stop: function (e){
            updateEvSliderValue(id, (100 - e.value));
            document.getElementById(id + "-eventState").value = '';
        },
        update: function (e){
            $('#' + id).addClass('doNotRefresh');
            document.getElementById(id + '-label').innerHTML = (100 - e.value) + "%";
            nativeMessage('startButtonPress');
            if(document.getElementById(id + "-eventState").value === 'drag'){
                // waiting for 'stop' event
            } else {
                updateEvSliderValue(id, (100 - e.value));
            }
        }
    });
    $('#' + id).roundSlider("disable");
    // prevent misaligned handle
    setTimeout(function(){
        document.getElementById(id).getElementsByClassName('rs-handle').item(0).style.display = 'block';
    }, 10);
}

let initSliderFunctionsByName = [];
initSliderFunctionsByName['initEvChargeSlider'] = initEvChargeSlider;

function enableSlider(id){
    $('#' + id).addClass('doNotRefresh');
    $('#' + id).roundSlider("enable");
    setTimeout(function(){
        if(document.getElementById(id + "-eventState").value != 'drag'){
            $('#' + id).removeClass('doNotRefresh');
            $('#' + id).roundSlider("disable");
        }
    }, 6000);
}

function evChargeLimit(link, value){
    if(!value.startsWith('#')){
        let linkComplete = link + value;
        submitContent(linkComplete);
    }
}

function updateEvSliderValue(id, value) {
    document.getElementById(id + '-status').style.color = "red";
    submitContent(document.getElementById('val_update_slider_' + id).value + value);
    $('#' + id).removeClass('doNotRefresh');
}