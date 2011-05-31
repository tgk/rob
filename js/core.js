(function() {
  var SZ, angle, labels, main, whoami;
  SZ = 20;
  angle = {
    north: 0,
    east: 90,
    south: 180,
    west: 270
  };
  labels = {
    fire: "!",
    "turn-left": "&#8634",
    "turn-right": "&#8634",
    forward: "&uarr;",
    backward: "&darr;"
  };
  whoami = 0;
  $.fn.addPlayer = function(data) {
    return $('<div class="player"><div style="-webkit-transform: rotate(' + angle[data.direction] + 'deg); ">&uarr;</div></div>').css({
      bottom: SZ * data.y,
      left: SZ * data.x
    }).appendTo(this);
  };
  main = function() {
    return $.get('/game-state', function(state) {
      var actions;
      $('.board').empty().addPlayer(state.players[0]).addPlayer(state.players[1]);
      actions = $('.actions').empty();
      return $(state.players[whoami].deck).each(function(i) {
        return $('<li/>').html(labels[this.type] || this.type).data('index', i).appendTo(actions);
      });
    });
  };
  $(function() {
    console.debug("??");
    $('.actions li').live('mousedown', function(ev) {
      var d, i, settings;
      i = $(this).data('index');
      d = '{"player-id": ' + whoami + ', "card-number": ' + i + '}';
      settings = {
        url: '/play-card',
        type: "post",
        contentType: 'application/json',
        data: d
      };
      console.debug(settings);
      return $.ajax(settings);
    });
    return setInterval(main, 50);
  });
}).call(this);
