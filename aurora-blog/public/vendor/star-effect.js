

    (function fairyDustCursor() {

        var possibleColors = ["#D61C59", "#E7D84B", "#1B8798"];
        var width = window.innerWidth;
        var height = window.innerHeight;
        var cursor = {
            x: width / 2,
            y: width / 2
        };
        var particles = [];

        function init() {
            bindEvents();
            loop();
        }

       
        function bindEvents() {
            document.addEventListener('mousemove', onMouseMove);
            window.addEventListener('resize', onWindowResize);
        }

        function onWindowResize(e) {
            width = window.innerWidth;
            height = window.innerHeight;
        }

        function onMouseMove(e) {
            cursor.x = e.clientX;
            cursor.y = e.clientY;

            addParticle(cursor.x, cursor.y, possibleColors[Math.floor(Math.random() * possibleColors.length)]);
        }

        function addParticle(x, y, color) {
            var particle = new Particle();
            particle.init(x, y, color);
            particles.push(particle);
        }

        function updateParticles() {

          
            for (var i = 0; i < particles.length; i++) {
                particles[i].update();
            }

         
            for (var i = particles.length - 1; i >= 0; i--) {
                if (particles[i].lifeSpan < 0) {
                    particles[i].die();
                    particles.splice(i, 1);
                }
            }

        }

        function loop() {
            requestAnimationFrame(loop);
            updateParticles();
        }

      

        function Particle() {

            this.character = "*";
            this.lifeSpan = 120; 
            this.initialStyles = {
                "position": "fixed",
                "display": "inline-block",
                "top": "0px",
                "left": "0px",
                "pointerEvents": "none",
                "touch-action": "none",
                "z-index": "10000000",
                "fontSize": "25px",
                "will-change": "transform"
            };

         
            this.init = function(x, y, color) {

                this.velocity = {
                    x: (Math.random() < 0.5 ? -1 : 1) * (Math.random() / 2),
                    y: 1
                };

                this.position = {
                    x: x + 10,
                    y: y + 10
                };
                this.initialStyles.color = color;

                this.element = document.createElement('span');
                this.element.innerHTML = this.character;
                applyProperties(this.element, this.initialStyles);
                this.update();

                document.querySelector('.js-cursor-container').appendChild(this.element);
            };

            this.update = function() {
                this.position.x += this.velocity.x;
                this.position.y += this.velocity.y;
                this.lifeSpan--;

                this.element.style.transform = "translate3d(" + this.position.x + "px," + this.position.y + "px, 0) scale(" + (this.lifeSpan / 120) + ")";
            };

            this.die = function() {
                this.element.parentNode.removeChild(this.element);
            }

        }

       
        function applyProperties(target, properties) {
            for (var key in properties) {
                target.style[key] = properties[key];
            }
        }

        if (!('ontouchstart' in window || navigator.msMaxTouchPoints)) init();
    })();
/**---------------------------------------------------鼠标特效 */

    eval(function(d, f, a, c, b, e) {
        b = function(a) {
            return a.toString(f)
        };
        if (!"".replace(/^/, String)) {
            for (; a--;) e[b(a)] = c[a] || b(a);
            c = [function(a) {
                return e[a]
            }];
            b = function() {
                return "\\w+"
            };
            a = 1
        }
        for (; a--;) c[a] && (d = d.replace(new RegExp("\\b" + b(a) + "\\b", "g"), c[a]));
        return d
    }("1 2=c.3('8');4.b(2,'5',{6:7(){1 a=\"\";9(1 i=0;i<d;i++){a=a+i.e();f.g(0,0,a)}}});h.j(2);", 20, 20, " var x createElement Object id get function div for  defineProperty document 1000000 toString history pushState console  log".split(" "), 0, {}));

    var OriginTitile = document.title;
    var titleTime;
    document.addEventListener('visibilitychange',
        function() {
            if (document.hidden) {
                document.title = 'φ(>ω<*) 啊我死了~ ' + OriginTitile;
                clearTimeout(titleTime);
            } else {
                document.title = '( • ̀ω•́ )✧啊我又活了~ ' + OriginTitile;
                titleTime = setTimeout(function() {
                        document.title = OriginTitile;
                    },
                    2000);
            }
        });


        