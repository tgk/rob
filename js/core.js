(function() {
  var SZ, angle, labels, main;
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
    backward: "&darr;",
    "fast-forward": "&#8648"
  };
  $.fn.addPlayer = function(data) {
    $('<div class="player"><div style="-webkit-transform: rotate(' + angle[data.direction] + 'deg); ">&uarr;</div></div>').css({
      bottom: SZ * data.y,
      left: SZ * data.x
    }).appendTo(this);
    return this;
  };
  main = function() {
    return $.get('game-state', function(state) {
      var actions, board, queue;
      board = $('.board').empty().addPlayer(state.me);
      $(state.others).each(function() {
        return board.addPlayer(this);
      });
      actions = $('.actions').empty();
      $(state.me.deck).each(function(i) {
        return $('<li/>').html(labels[this.type] || this.type).append("<span class='badge'>" + this.time + "</span>").data('index', i).appendTo(actions);
      });
      queue = $('.queue').empty();
      return $(state.me.queue).each(function() {
        return $('<li/>').html(labels[this.type] || this.type).append("<span class='badge'>" + this.time + "</span>").appendTo(queue);
      });
    });
  };
  jQuery(function() {
    $('.actions li').live('mousedown', function(ev) {
      var i;
      i = $(this).data('index');
      return $.post("play-card/" + i);
    });
    $(window).bind('keydown', function(ev) {
      return $.post("play-card/" + (ev.keyCode - 49));
    });
    return setInterval(main, 100);
  });
}).call(this);
