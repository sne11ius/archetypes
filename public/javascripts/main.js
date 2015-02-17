$('.archetypes-table .details').hide();
$('.archetypes-table col.details').remove();
$('.archetypes-table [data-href]').click(function() {
    window.location.href = $(this).find("a").attr("href");
});


$(document).ready(function() {
  if ('newest' == $("#version").val()) {
    $('#newest-checkbox').attr('checked', 'checked');
    $("#version").prop('readonly', 'readonly');
  }
  $('#newest-checkbox').on('click', function() {
    var disabled = $(this).is(':checked');
    $("#version").prop('readonly', disabled ? 'readonly' : '');
    $("#version").val(disabled ? 'newest' : '');
  });
  $('#about').on('click', function() {
    $(this).hide();
  });
  $('#about-hint').on('click', function() {
    $('#about').show();
  });
});
