function loadMetaData(groupId, artifactId, version) {
  console.log('Loading metadata for ' + groupId + ' > ' + artifactId + ' > ' + version);
  var url = '/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/loadMetaData';
  $.getJSON(url, function(data) {
    console.log('Should be no content...');
    console.log(data);
    /*
    $('#file-browser').fileTree({root: '/', script: 'connectors/jqueryFileTree.php' }, function(file) { 
      alert(file);
    });
    */
    $('#file-browser').fileTree({script: '/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/browse' }, function(file) { 
      alert(file);
    });
    $('#wait-hint').hide();
  });
}
