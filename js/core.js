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
    "turn-right": "&#8635",
    forward: "&uarr;",
    backward: "&darr;",
    "fast-forward": "&#8648"
  };
  $.fn.addPlayer = function(data, clazz) {
    $('<div class="player"><div style="-webkit-transform: rotate(' + angle[data.direction] + 'deg); ">&uarr;</div></div>').addClass(clazz).css({
      bottom: SZ * data.y,
      left: SZ * data.x
    }).appendTo(this);
    return this;
  };
  $.fn.addGoal = function(data) {
    $("<div class='goal' />").html('&hearts;').css({
      bottom: SZ * data.y,
      left: SZ * data.x
    }).appendTo(this);
    return this;
  };
  main = function() {
    return $.get('game-state', function(state) {
      var actions, board, queue;
      if (state.winnerinfo !== 'undecided') {
        $('.result').html("<h1>" + state.winnerinfo + "</h1>").css('display', 'block');
      }
      board = $('.board').empty().addPlayer(state.me, "me");
      board.addGoal(state.goal);
      $(state.others).each(function() {
        return board.addPlayer(this, "other");
      });
      actions = $('.actions').empty();
      $(state.me.deck).each(function(i) {
        return $('<li/>').html(labels[this.type] || this.type).css({
          'background-color': "hsl(" + (100 - 4 * this.time) + ", 100%, 75%)"
        }).append("<span class='badge'>" + this.time + "</span>").data('index', i).appendTo(actions);
      });
      queue = $('.queue').empty();
      return $(state.me.queue).each(function() {
        return $('<li/>').html(labels[this.type] || this.type).append("<span class='badge'>" + this.time + "</span>").css({
          'background-color': "hsl(" + (100 - 4 * this.time) + ", 100%, 75%)"
        }).appendTo(queue);
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
