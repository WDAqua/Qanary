// Extracted from the inline <script> in templates/lib/header.html (issue #6).
// Server-injected data (the component list) is provided by the small inline
// shim in header.html as window.qanaryComponentList.
       	// TODO: move minimumNumberOfSelectedComponents to config
       	minimumNumberOfSelectedComponents = 1;
       	
        $(function() {
        		// make the li elements sortable (by drag and drop)
                $( "#sortable" ).sortable();
                // disable text selection on draggable elements 
                $( "#sortable" ).disableSelection();
                // change style of selected elements
                $( "#sortable li input").click( function(){
                        if( $(this).prop("checked") == true ){
                                $(this).addClass("selected");
                                $(this).parents("li").addClass("selected");     
                        } else {
                                $(this).removeClass("selected");
                                $(this).parents("li").removeClass("selected");
                        }                       
                    	checkInputAndOptionallyDisableSubmitButton("#submit");
                });
                // check the value of the question input -> might disable the submit button
                $("#question").focusout( function(){
                	checkInputAndOptionallyDisableSubmitButton("#submit");
                });
                // check the value of the question input, while typing -> might disable the submit button
                $("#question").keypress( function(){
                	checkInputAndOptionallyDisableSubmitButton("#submit");
                });
                // check the selections within the component list
                $("#submit").mouseover(function(){
                	checkInputAndOptionallyDisableSubmitButton("#submit");
                });

                $("#results").hide(); // hide results table

                // add AJAX action to form
                $("#startForm").submit(function(event){
                	event.preventDefault(); // prevent default action 
                	$("#submit").attr("disabled", true); // disable button
                	$("#submit").addClass("inprogress"); // change cursor
                	$("#results").slideUp(500); // hide last results with animation
                	$("#errormessage").slideUp(500); // hide last ERROR message with animation
                	$("#errormessage").html();
                    removeAnnotationCount(); // clear old annotation count values
                    message = "";
                    $.ajax({
                		url : $(this).attr("action"), // get action url
                		type: $(this).attr("method"), //get method
                		data: $(this).serialize() // encode form elements 
                	}).done(function(response){ 
                		console.log(response);
                    	$("#submit").attr("disabled", false); // reactivate button
                    	$("#submit").removeClass("inprogress");
                		$("#endpoint").html(response.endpoint);
                		$("#questionuri").html(response.question);
                		$("#inGraph").html(response.inGraph);
                		$("#protocol_link").html($("<a></a>").text("process details").prop("href","/questionanswering/" + response.inGraph).prop("target","_blank"));
                		$("#all_annotation_link").html($("<a></a>").text("all annotations").prop("href","/components/" + response.inGraph + "/*").prop("target","_blank"));
                		$("#outGraph").html(response.outGraph);
                    	$("#results").slideDown("fast"); // show results with animation
                    	$("#results").show();
                        getNumberOfAnnotations(response.outGraph); // get new number of annotations
                	}).error(function (jqXHR, textStatus) {
                		console.error("textStatus: ", textStatus);
                		console.error("jqXHR: ", jqXHR);
                        $("#submit").attr("disabled", false); // reactivate button
                        $("#submit").removeClass("inprogress");
                        jsonMessage = jqXHR.responseJSON["message"];
                        textMessage = jqXHR.responseText;
                        if (jsonMessage) {
                        	console.log("jsonMessage:", jsonMessage);                        	
                        	coreMessage = jsonMessage.substr(0, jsonMessage.indexOf("|", jsonMessage.indexOf(":")) + 1)
                        } else if (textMessage){
                        	console.log("textMessage:",textMessage);
                       		coreMessage = textMessage;
                        } else if (textStatus){
                        	console.log("textStatus:",textStatus);
                       		coreMessage = textStatus;
                        } else {
                        	console.log("error message: none found");
                        	coreMessage = "";
                        }
                        message = "ERROR: The process failed.\nPlease check your implementation!\n<p class=\"coreMessage\">" + coreMessage + "</p>See your browser developer console for details."; 
	                	$("#errormessage").html(message);
                    	$("#errormessage").slideDown("fast"); // show results with animation
                    	$("#errormessage").show();
                        // alert(message);
                        console.error(message);
                	}).fail(function (jqXHR, textStatus) {
                		if(message == ""){ // only do this if it was not done in the error() block
	                		console.error("textStatus: ", textStatus);
	                		console.error("jqXHR: ", jqXHR);
	                        $("#submit").attr("disabled", false); // reactivate button
	                        $("#submit").removeClass("inprogress");
	                        jsonMessage = jqXHR.responseJSON["message"];
	                        textMessage = jqXHR.responseText;
	                        if (jsonMessage) {
	                        	console.log("jsonMessage:", jsonMessage);                        	
	                        	coreMessage = jsonMessage.substr(0, jsonMessage.indexOf("|", jsonMessage.indexOf(":")) + 1)
	                        } else if (textMessage){
	                        	console.log("textMessage:",textMessage);
	                       		coreMessage = textMessage;
	                        } else if (textStatus){
	                        	console.log("textStatus:",textStatus);
	                       		coreMessage = textStatus;
	                        } else {
	                        	console.log("error message: none found");
	                        	coreMessage = "";
	                        }
	                        message = "ERROR: The process failed.\nPlease check your implementation!\n<p class=\"coreMessage\">" + coreMessage + "</p>See your browser developer console for details."; 
		                	$("#errormessage").html(message);
	                    	$("#errormessage").slideDown("fast"); // show results with animation
	                    	$("#errormessage").show();
	                        // alert(message);
	                        console.error(message);
						}
                	});
                });
        });

        // remove any AnnotationCount components so that props can be updated correctly
        function removeAnnotationCount() {
            let components = Object.keys(window.qanaryComponentList || {})
            for (i = 0; i < components.length; i++) {
                let element = document.getElementById("annotationcount"+components[i]);
                if(element){
                    ReactDOM.unmountComponentAtNode(element);
                }
            }
        }

        // mount individual AnnotationCount to every component checklist item
        function getNumberOfAnnotations(graph) {
            console.log(graph)
            let components = Object.keys(window.qanaryComponentList || {});
            for (i = 0; i < components.length; i++) {
                console.log(graph)
                elementID = "annotationcount" + components[i];
                props = {
                    component: components[i],
                    graph: graph,
                    fetched: false,
                    annotationID: (elementID)
                }
                let annotationElement = e(AnnotationCount,props);
                ReactDOM.render(annotationElement, document.getElementById(elementID));
            }
        }
        
        function checkInputAndOptionallyDisableSubmitButton(buttonID){
        	// disable button if the minimum number of components was not activated or question was not given
        	if(!checkComponentList("#sortable",minimumNumberOfSelectedComponents)){
        		$("#submit").attr("disabled", true);
        	} else if(!checkQuestionUriInput($("#question"))){
        		$("#submit").attr("disabled", true);
        	} else {
        		// everything ok
        		$("#submit").attr("disabled", false);
        	}
        }
        
        function checkQuestionUriInput(inputelement){
                if( $(inputelement).val().trim() == "" ){
                        $(inputelement).addClass("isempty");
                        return false;
                } else {
                        $(inputelement).removeClass("isempty");
                        return true;
                } 
        }
        
        function checkComponentList(blockElementContainingCheckboxes, minimumNumberOfSelectedComponents){
			if( $(blockElementContainingCheckboxes).find("input:checked").length >= minimumNumberOfSelectedComponents){
                $(blockElementContainingCheckboxes).removeClass("missingcomponentselection");
                return true;
			} else {
				$(blockElementContainingCheckboxes).addClass("missingcomponentselection");
				return false;
			}	
        }
        
		function filterComponentList() {
		  // Declare variables
		  var input, filter, ul, li, txtValue, hidden, speed;
		  speed = 333;
		  input = document.getElementById('componentfilterinput');
		   
		  filter = input.value.toUpperCase();
		  ul = document.getElementById("sortable");
		  li = ul.getElementsByTagName("li");

		  showEverything = false;
		  if( filter.trim().length == 0 ){
		  	showEverything = true;
		  }
		  
		  // Loop through all list items, and hide those who don't match the search query
		  hidden = 0;
		  for (i = 0; i < li.length; i++) {
		  	if( showEverything ){
		  		$(li[i]).fadeIn(speed);
		  	} else {
			    txtValue = li[i].innerText;
			    if (txtValue.toUpperCase().indexOf(filter) > -1) {
			      $(li[i]).fadeIn(speed);
			    } else {
			      $(li[i]).fadeOut(speed);
			      hidden++;
			    }
		    }
		  }

		  console.debug("filter", filter, "showEverything", showEverything, "li.length", li.length, "hidden", hidden);
		}   
