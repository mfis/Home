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
            for (let i = this.options.min; i <= this.options.max; i += 12.5) {
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

function initSlider(id){
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
            if(document.getElementById(id + "-eventState").value == 'drag'){
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

function enableSlider(id){
    console.log('ID=' + id, $('#' + id))
    $('#' + id).addClass('doNotRefresh');
    $('#' + id).roundSlider("enable");
    console.log('handle', document.getElementById(id).getElementsByClassName('rs-handle').item(0).style)
    setTimeout(function(){
        if(document.getElementById(id + "-eventState").value != 'drag'){
            $('#' + id).removeClass('doNotRefresh');
            $('#' + id).roundSlider("disable");
        }
    }, 6000);
}

function updateEvSliderValue(id, value) {
    document.getElementById(id + '-status').style.color = "red";
    submitContent(document.getElementById('val_update_slider_' + id).value + value);
    $('#' + id).removeClass('doNotRefresh');
}