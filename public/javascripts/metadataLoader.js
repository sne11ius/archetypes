/*
String.prototype.endsWith = String.prototype.endsWith || function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

function loadMetaData(groupId, artifactId, version) {
  console.log('Loading metadata for ' + groupId + ' > ' + artifactId + ' > ' + version);
  var url = '/mit/archetypes/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/loadMetaData';
  $.getJSON(url, function() {
    window.setTimeout(function() {
      var $readmes = $('[rel*="readme"]');
      if ($readmes.length) {
        $readmes.click();
      } else {
        $('[rel*="pom"]').click();
      }
    }, 500);
    $('#file-browser').fileTree({script: '/mit/archetypes/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/browse' }, function(file) {
      var fileUrl = '/mit/archetypes/rest/archetypes/' + groupId + '/' + artifactId + '/' + version + '/files' + file
      $.get(fileUrl, function(data) {
        if (file.endsWith('.md') || file.endsWith('.markdown')) {// || file.endsWith('.txt') || file.toLowerCase().endsWith('readme')) {
          $('#file-content').html(markdown.toHTML(data));
        }else {
          $('#file-content').html('<pre class="prettyprint linenums">' + data + '</pre>');
          prettyPrint();
        }
      });
    });
    $('#wait-hint').hide();
    $('#details').show();
  });
}
*/