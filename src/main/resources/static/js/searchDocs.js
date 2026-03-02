$(document).ready(function() {
            $("#fileUploadForm").submit(function(event) {
                event.preventDefault();
                var formData = new FormData(this);
                $.ajax({
                    url: '/api/index',
                    type: 'POST',
                    data: formData,
                    contentType: false,
                    processData: false,
                    success: function(data) {
                        alert(window.i18n.fileIndexed);
                    }
                });
            });

            $("#luceneQueryLanguage").submit(function(event) {
                event.preventDefault();
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
                        result.contentSr ||
                        result.contentEn ||
                        result.contentDe ||
                        result.contentRu ||
                        result.contentFr ||
                        result.contentIt ||
                        result.contentEs ||
                        result.contentPt ||
                        result.contentUk ||
                        "unknown";

                    switch(contentToDisplay) {
                      case result.contentSr:
                          imgToDisplay="sr_icon";
                          break;
                      case result.contentEn:
                          imgToDisplay="en_icon";
                          break;
                      case result.contentDe:
                          imgToDisplay="de_icon";
                          break;
                      case result.contentRu:
                          imgToDisplay="ru_icon";
                          break;
                      case result.contentFr:
                          imgToDisplay="fr_icon";
                          break;
                        case result.contentIt:
                          imgToDisplay="it_icon";
                          break;
                      case result.contentEs:
                          imgToDisplay="es_icon";
                          break;
                        case result.contentPt:
                          imgToDisplay="pt_icon";
                          break;
                      case result.contentUk:
                          imgToDisplay="uk_icon";
                          break;
                      default:
                          imgToDisplay="unknown_icon";
                    }

                    var pathToImg = "/img/" + imgToDisplay + ".png";

                    var card = `<div class="col-md-6">
                                    <div class="card">
                                        <div class="card-body">
                                            <h5 class="card-title">${result.title}</h5>
                                            <p class="card-text">${contentToDisplay}</p>
                                            <div id="cont-img-container">
                                                <img class="round" id="card-img" src=${pathToImg} alt=${imgToDisplay} />
                                            </div>
                                            <a href="http://localhost:8080/api/file/${result.serverFilename}" class="btn btn-primary" download="${result.title.replace(/\s+/g, '-')}">Download</a>
                                        </div>
                                    </div>
                                </div>`;
                    resultsDiv.append(card);
                });

                if (data.content.length > 0) {
                    $("#resultsContainer").show();
                }
                else{
                    alert(window.i18n.noResults);
                }
                resultsDiv.show();
            }
        });