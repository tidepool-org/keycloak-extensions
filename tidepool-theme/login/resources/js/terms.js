// Show or hide an element by id. `mode` is the display value used when visible
// (e.g. "flex" for the .pf-v5-c-check rows, "block" for the sorry message).
function show(id, visible, mode) {
  document.getElementById(id).style.display = visible ? mode : "none";
}

function updatePatientTermsForm() {
  let isEnabled = false;
  let termsVisible = false;
  let termsChildVisible = false;
  let sorryVisible = false;

  let termsAccepted = document.getElementById("terms").checked;
  let termsChildAccepted = document.getElementById("terms-child").checked;
  let age = document.querySelector('input[name="age"]:checked').value;

  if (age === ">18") {
    termsVisible = true;
    isEnabled = termsAccepted
  } else if (age === "13-17") {
    termsVisible = true;
    termsChildVisible = true;
    isEnabled = termsAccepted && termsChildAccepted
  } else if (age === "<13") {
    sorryVisible = true;
  }

  // terms-wrapper / terms-child-wrapper are .pf-v5-c-check flex rows, so show
  // them as "flex" (not "block") to preserve the checkbox/label layout.
  show("terms-wrapper", termsVisible, "flex");
  show("terms-child-wrapper", termsChildVisible, "flex");
  show("terms-sorry", sorryVisible, "block");

  document.getElementById("kc-accept").disabled = !isEnabled;
}

function updateClinicianTermsForm() {
  let isEnabled = document.getElementById("terms").checked;
  document.getElementById("kc-accept").disabled = !isEnabled;
}