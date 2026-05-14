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

  if (termsVisible) {
    document.getElementById("terms-wrapper").style.display = "block"
  } else {
    document.getElementById("terms-wrapper").style.display = "none"
  }

  if (termsChildVisible) {
    document.getElementById("terms-child-wrapper").style.display = "block"
  } else {
    document.getElementById("terms-child-wrapper").style.display = "none"
  }

  if (sorryVisible) {
    document.getElementById("terms-sorry").style.display = "block"
  } else {
    document.getElementById("terms-sorry").style.display = "none"
  }

  document.getElementById("kc-accept").disabled = !isEnabled;
}

function updateClinicianTermsForm() {
  let isEnabled = document.getElementById("terms").checked;
  document.getElementById("kc-accept").disabled = !isEnabled;
}