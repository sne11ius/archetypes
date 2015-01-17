function resetForm() {
  $('#groupId-input').val('');
  $('#artifactId-input').val('');
  $('#description-input').val('');
  $('#version-input').val('');
  $('#newest-checkbox').attr('checked', false);
}

function makeUrl() {
  var groupId = $('#groupId-input').val(),
      artifactId = $('#artifactId-input').val(),
      description = $('#description-input').val()
      version = $('#newest-checkbox').is(':checked')
              ? 'newest'
              : $('#version-input').val()
      map = {};
  if ('' !== groupId) {
    map['groupId'] = groupId;
  }
  if ('' !== artifactId) {
    map['artifactId'] = artifactId;
  }
  if ('' !== description) {
    map['description'] = description;
  }
  if ('' !== version) {
    map['version'] = version;
  }
  var queries = [];
  for (var i in map) {
    queries.push(i + '=' + map[i]); 
  }
  var queryString = '?' + queries.join('&')
  if (1 == queryString.length) {
    return '';
  } else {
    return './rest/archetypes' + queryString;
  }
}

function updateList() {
  $('#list-container').html('');
  var url = makeUrl();
  $.getJSON(url, function(data) {
    var html = '<table><col width="20%"><col width="20%"><col width="10%"><col width="50%"><tr><th class="groupId">groupId</th><th class="artifactId">artifactId</th><th class="version">version</th><th class="description">description</th></tr>';
    for (var i = 0; i < data.length; ++i) {
      html + '<tr>';
      html += '<td class="groupId">' + data[i].groupId + '</td>';
      html += '<td class="artifactId">' + data[i].artifactId + '</td>';
      html += '<td class="version">' + data[i].version + '</td>';
      html += '<td class="description">' + ('undefined' === typeof data[i].description ? '' : data[i].description) + '</td>';
      html += '</tr>';
    }
    html += '</table>';
    $('#list-container').html(html);
  });
}

$(document).ready(function() {
  $('#newest-checkbox').on('click', function() {
    $("#version-input").prop('disabled', $(this).is(':checked'));
    updateList();
  });
  $('.form-line input').on('keyup', function() {
    updateList();
  });
  $('.example-1 a').on('click', function() {
    resetForm();
    $('#artifactId-input').val('commons-');
    updateList();
  });
  $('.example-2 a').on('click', function() {
    resetForm();
    $('#description-input').val('jee7 webapp');
    $('#newest-checkbox').attr('checked', true);
    $("#newest-checkbox").prop("checked", true);
    updateList();
  });
  $('.example-3 a').on('click', function() {
    resetForm();
    $('#groupId-input').val('com.airhacks');
    $('#newest-checkbox').attr('checked', true);
    $("#newest-checkbox").prop("checked", true);
    updateList();
  });
});
