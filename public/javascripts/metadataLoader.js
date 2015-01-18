function loadMetaData(groupId, artifactId, version) {
  console.log('Loading metadata for ' + groupId + ' > ' + artifactId + ' > ' + version);
  var url = '/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/loadMetaData';
  $.getJSON(url, function() {
    $('#file-browser').fileTree({script: '/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/browse' }, function(file) {
      var fileUrl = '/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/files' + file
      //alert(fileUrl)
      $.get(fileUrl, function(data) {
        $('#file-content').html('<pre>' + data + '</pre>');
      });
      //alert(file);
    });
    $('#wait-hint').hide();
  });
}
