$(document).ready(
				function() {
					$("#locales").change(
							function() {
								var selectedOption = $('#locales').val();
								var location = window.location.href;
								var newlocation = '';

								if (selectedOption != '') {
									var params = new window.URLSearchParams(window.location.search);
									var localeParam = params.get('locale');
									if(localeParam!=null){
										var positionStart = location.indexOf('locale=')+7;
										var positionEnd = positionStart+localeParam.length;
										var locationStartStr = location.substring(0,positionStart);
										var locationEndStr = location.substring(positionEnd);
										newlocation = locationStartStr + selectedOption +locationEndStr;

									}else {
										if(location.indexOf('?')!=-1)
											newlocation = location +'&locale='+ selectedOption;
										else
											newlocation = location +'?locale='+ selectedOption;
									}

									window.location.replace(newlocation);
								}
							});
				});