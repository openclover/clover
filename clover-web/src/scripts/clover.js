// define all projects to load using JSON

function processClover(json) {
    var eles = $$('#' + json.title + ' td');

    var fmtPc = (Math.round(json.stats.TotalPercentageCovered*10)/10) + "%";

    // project title
    insertSortValue(json.title, eles[0]);    

    // Uncovered elements
    insertSortValue(json.stats.UncoveredElements, eles[1]);
    eles[1].appendChild(document.createTextNode(json.stats.UncoveredElements));

    // Complexity
    insertSortValue(json.stats.Complexity, eles[2]);
    eles[2].appendChild(document.createTextNode( json.stats.Complexity ));

    // % TPC
    insertSortValue(json.stats.TotalPercentageCovered, eles[3]);
    eles[3].appendChild(document.createTextNode( fmtPc ));
    eles[3].className = "strong";

    // Bar
    var bar = $('barchart').cloneNode(true);
    bar.style.display = "";
    var barsPos = bar.select('.barPositive');
    var barPos = barsPos[0];
    barPos.style.width = fmtPc;    
    eles[4].appendChild(bar);

    // display history on row mouseover
    $(json.title).onmouseover = function () { displayHistory(json.title) };
    $(json.title).className = "highlight";
    $('historyLink').href = getHistoryHref(json.title);
}

function getImgPath(project) {
    return "http://clover.atlassian.com/browse/" + project + "/img/chart1.jpg";
}

function getHistoryHref(project) {
    return "http://clover.atlassian.com/browse/" + project + "/historical.html";
}


var cloverImgs = {"guice":true};

function displayHistory(project) {
    var img = $("imgpreview");
    img.src = getImgPath(project);
    $("imgtitle").innerHTML = "Historical Coverage for " + project;
//    if (cloverImgs[project]) { // DETECTING when an image has loaded doesn't work too well.
//        return;
//    }
//    Effect.Fade(img);
//    img.onload = function () {
//        Effect.Appear(this);
//        cloverImgs[project] = true;
//    };
//    $("imgpane").className = "splash";
}

function insertSortValue(sortValue, ele) {
    var sortValueEle = $('sortvalue').cloneNode(true);
    sortValueEle.id = "";
    sortValueEle.appendChild(document.createTextNode( sortValue ));
    ele.appendChild(sortValueEle);
}

function sortByCoverage() {
    return ts_resortTable($('tpc'), 'number', 3);
}