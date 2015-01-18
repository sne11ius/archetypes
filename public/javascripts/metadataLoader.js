function loadMetaData(groupId, artifactId, version) {
  console.log('Loading metadata for ' + groupId + ' > ' + artifactId + ' > ' + version);
  var url = '/mit/archetypes/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/loadMetaData';
  $.getJSON(url, function() {
    $('#file-browser').fileTree({script: '/mit/archetypes/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/browse' }, function(file) {
      var fileUrl = '/mit/archetypes/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/files' + file
      $.get(fileUrl, function(data) {
        $('#file-content').html('<pre class="prettyprint linenums">' + data + '</pre>');
        prettyPrint();
      });
    });
    $('#wait-hint').hide();
    $('#details').show();
  });
}