$(document).ready(function() {
            $("#fileUploadForm").submit(function(event) {
                event.preventDefault();

                $("#btnSubmit").prop("disabled", true);

                var formData = new FormData(this);
                $.ajax({
                    url: '/api/index',
                    type: 'POST',
                    data: formData,
                    contentType: false,
                    processData: false,
                    success: function(data) {
                        alert('The file was uploaded and indexed successfully.');
                        $("#fileUploadForm input[type='file']").val('');
                    },
                    error: function() {
                        alert('An error occurred during the attempt to upload the file. Please try again later.');
                    },
                    complete: function(jqXHR, textStatus) {
//                        console.log("AJAX finished with status:", textStatus);
                        $("#btnSubmit").prop("disabled", false);
                    }
                });
            });

            $("#luceneQueryLanguage").submit(function(event) {
                event.preventDefault();
                $("#btnSubmitLuceneQueryLanguage").prop("disabled", true);

                var query = {
                    keywords: $("#query").val().split(" ")
                };
                var url = '/api/search/' + ($("#queryTypeAdvanced").is(':checked') ? 'advanced' : 'simple') + '?isKnn=' + ($("#queryTypeKNN").is(':checked') ? 'true' : 'false');
                $.ajax({
                    url: url,
                    type: 'POST',
                    data: JSON.stringify(query),
                    contentType: 'application/json',
                    success: function(data) {
                        displayResults(data);
                    },
                     error: function() {
                         alert('An error occurred during search. Please try again later.');
                     },
                     complete: function(jqXHR, textStatus) {
//                        console.log("AJAX finished with status:", textStatus);
                         $("#btnSubmitLuceneQueryLanguage").prop("disabled", false);
                     }
                });
            });

            function displayResults(data) {

                $("#resultsContainer").hide();
                var resultsDiv = $("#results");
                resultsDiv.empty();

                data.content.forEach(function(result) {

                    var imgToDisplay;
                    var contentToDisplay =
                        result.contentSr || result.contentEn || result.contentDe ||
                        result.contentRu || result.contentFr || result.contentIt ||
                        result.contentEs || result.contentPt || result.contentUk ||
                        "unknown";
//                        console.log("RAW CONTENT:", contentToDisplay);

                    switch(contentToDisplay) {
                        case result.contentSr: imgToDisplay = "sr_icon"; break;
                        case result.contentEn: imgToDisplay = "en_icon"; break;
                        case result.contentDe: imgToDisplay = "de_icon"; break;
                        case result.contentRu: imgToDisplay = "ru_icon"; break;
                        case result.contentFr: imgToDisplay = "fr_icon"; break;
                        case result.contentIt: imgToDisplay = "it_icon"; break;
                        case result.contentEs: imgToDisplay = "es_icon"; break;
                        case result.contentPt: imgToDisplay = "pt_icon"; break;
                        case result.contentUk: imgToDisplay = "uk_icon"; break;
                        default: imgToDisplay = "unknown_icon";
                    }

                    var flagPath = "/img/" + imgToDisplay + ".png";
                    var pdfFilename = result.serverFilename;
                    var temp = pdfFilename.substring(0, pdfFilename.lastIndexOf('.'));
//                    console.log(temp);
                    var temp2 = temp + "_preview";
//                    console.log(temp2);
                    var imgFilename = temp2 + ".png";
//                    console.log(imgFilename);


                    // Flag icon, based on results' content language
                    var flagImageHtml = `
                        <div class="mb-2" id="cont-img-container">
                            <img class="round" src="${flagPath}" alt="${imgToDisplay}" style="width: 30px; height: 30px;" />
                        </div>
                    `;

                    // Optional preview image
                    var previewImageHtml = `
                        <div class="mb-3 text-center">
                            <img class="preview-img"
                                 data-filename="${imgFilename}"
                                 alt="preview"
                                 style="max-height:200px; object-fit:cover;" />
                        </div>
                    `;

//                    console.log(imgFilename);

                    var card = `
                        <div class="col-md-6">
                            <div class="card mb-3">
                                <div class="card-body">
                                    ${previewImageHtml}
                                    <h5 class="card-title">${result.title}</h5>
                                    <p class="card-text">${contentToDisplay}</p>
                                    ${flagImageHtml}
                                    <a href="http://localhost:8080/api/file/${pdfFilename}" class="btn btn-primary" download="${result.title.replace(/\s+/g, '-')}">Download</a>
                                </div>
                            </div>
                        </div>
                    `;

                    resultsDiv.append(card);
                });

                // Load optional preview images via MinIO
                $(".preview-img").each(function() {
                    var imgElement = $(this);
                    var filename = imgElement.data("filename");

                    $.ajax({
                        url: "/api/file/preview/" + filename,
                        type: "GET",
                        success: function(response) {
                            imgElement.attr("src", response.url);
                        },
                        error: function() {
                            // Hide preview if not present
                            imgElement.hide();
                        }
                    });
                });

                if (data.content.length > 0) {
                    $("#resultsContainer").show();
                } else {
                    alert('No results were found.');
                }
                resultsDiv.show();
            }

            function loadImagePreviews() {

                $(".preview-img").each(function() {

                    var imgElement = $(this);
                    var filename = imgElement.data("filename");

                    $.ajax({
                        url: "/api/file/preview/" + filename,
                        type: "GET",
                        success: function(response) {
                            imgElement.attr("src", response.url);
                        },
                        error: function() {
                            imgElement.hide();
                        }
                    });
                });
            }
        });