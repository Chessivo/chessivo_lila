document.addEventListener('DOMContentLoaded', function() {
    var activityTab = document.querySelector('a[data-tab="activity"]');
    if (activityTab) {
        activityTab.addEventListener('click', function() {
            setTimeout(initForm, 100);  // Adjust the delay as needed to ensure the form is loaded
        });
    }
    initForm();
});

var isChecking = false;

function initForm() {
    var form = document.querySelector('main.importer form');
    if (form) { // Check if the form exists
        form.addEventListener('submit', function() {
            setTimeout(function() {
                form.innerHTML = lichess.spinnerHtml; // Assuming lichess.spinnerHtml is defined somewhere in your code
            }, 50);
        });

        if (window.FileReader) {
            var fileInput = form.querySelector('input[type=file]');
            fileInput.addEventListener('change', function () {
                var file = this.files[0];
                if (!file) return;
                var reader = new FileReader();
                reader.onload = function (e) {
                    var textarea = form.querySelector('textarea');
                    textarea.value = e.target.result;
                };
                reader.readAsText(file);
            });
        } else {
            var upload = form.querySelector('.upload');
            if (upload) {
                upload.remove();
            }
        }

		if(isChecking) {
			return;
		}
        var pgnTextarea = form.querySelector('textarea#form3-pgn');
        var sfModeWhite = document.querySelector("label[for='sf_mode_white']");
        var sfModeBlack = document.querySelector("label[for='sf_mode_black']");

        setInterval(function() {
            var pgnText = pgnTextarea.value;
            var whitePlayer = pgnText.match(/\[White "(.*?)"\]/);
            var blackPlayer = pgnText.match(/\[Black "(.*?)"\]/);

            sfModeWhite.innerText = whitePlayer ? `Analyze for White (${whitePlayer[1]})` : "Analyze for White";
            sfModeBlack.innerText = blackPlayer ? `Analyze for Black (${blackPlayer[1]})` : "Analyze for Black";
        }, 300);
		isChecking = true;
    }
}