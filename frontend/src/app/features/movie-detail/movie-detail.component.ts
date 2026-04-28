import {
  Component, OnInit, OnDestroy,
  inject, ChangeDetectorRef,
  ViewChild, ElementRef, HostListener
} from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MovieService }   from '../../core/services/movie.service';
import { MovieDetail }    from '../../core/models/movie.model';
import { HistoryService } from '../../core/services/history.service';
import { UserService } from '../../core/services/user.service';
import { CommentService } from '../../core/services/comment.service';
import { MovieComment} from '../../core/models/comment.model';
import { FormsModule } from '@angular/forms';
import { LanguageService } from '../../core/services/languaje.service';

type DownloadStatus = 'idle' | 'loading' | 'downloading' | 'error';

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [CommonModule, DecimalPipe, DatePipe, FormsModule],
  templateUrl: './movie-detail.component.html',
  styleUrl: './movie-detail.component.css',
})
export class MovieDetailComponent implements OnInit, OnDestroy {

	private route          = inject(ActivatedRoute);
	private router         = inject(Router);
	private movieService   = inject(MovieService);
	private cdr				= inject(ChangeDetectorRef);
	private historyService	= inject(HistoryService);
	private userService		= inject(UserService);
	private commentService = inject(CommentService);
	public langService = inject(LanguageService);
	

	@ViewChild('playerContainer') playerContainerRef!: ElementRef<HTMLDivElement>;
	@ViewChild('videoPlayer')     videoRef!:           ElementRef<HTMLVideoElement>;

	movie:         MovieDetail | null = null;
	isLoading      = true;
	private movieId = 0;
	user: any = null;
	private subtitleRequestId = 0;
	previousVolume = 1;

	downloadStatus:   DownloadStatus = 'idle';
	downloadMessage   = '';
	downloadProgress  = 0;
	playerVisible     = false;
	isReadyToResume   = false;
	resumePercent     = 0;
	_pendingResume    = false;
	private progressSub: any;
	private subtitleLoading = false;
	subtitleDelay: number = 0;
	private originalVttText: string | null = null;
    private currentBlobUrl: string | null = null;
	similarMovies: any[] = [];

	comments:            MovieComment[] = [];
	isLoadingComments    = false;
	newCommentText       = '';
	isSubmittingComment  = false;
	commentError         = '';

	isPlaying      = false;
	isStreamReady  = false;
	playedPercent  = 0;
	currentTimeStr = '0:00';
	durationStr    = '0:00';
	volume         = 1;
	
	isFavorite = false;
	isTogglingFavorite = false;

	subtitleUrl    = '';
	subtitleLang   = 'en';
	availableLangs = [
		{ code: 'en', label: 'English' },
		{ code: 'es', label: 'Español' },
		{ code: 'fr', label: 'Français' },
		{ code: 'de', label: 'Deutsch' },
		{ code: 'it', label: 'Italiano' },
		{ code: 'pt', label: 'Português' },
	];
	showLangSelector = false;

	private currentOffset    = 0;
	totalDuration    = 0;
	private lastTimeUpdate   = 0;
	private seekDebounce: any;
	private reconnectAttempts = 0;
	canSeekHover = false;
	hoverPercent  = 0;

	// ═══════════════════════════════════════════════════════════════════════════
	//  Lifecycle
	// ═══════════════════════════════════════════════════════════════════════════

	ngOnInit(): void {
		this.movieId = Number(this.route.snapshot.paramMap.get('id'));
		if (!this.movieId) {
			this.router.navigate(['/']);
			return;
		}

		this.loadUserProfile();

		this.loadFavoriteState();
		this.loadComments();
			
		this.movieService.getSimilarMovies(this.movieId).subscribe({
            next: (movies) => {
                this.similarMovies = movies.slice(0, 6);
                this.cdr.detectChanges();
            }
        });

		this.movieService.getMovieById(this.movieId).subscribe({
			next: (movie) => {
			this.movie         = movie;
			this.totalDuration = (movie.runtime ?? 0) * 60;
			this.isLoading     = false;
			this.cdr.detectChanges();
			},
			error: () => {
				this.isLoading = false;
				this.router.navigate(['/home']);
			}
		});

		this.historyService.getMovieProgress(this.movieId).subscribe({
			next:  (pct) => {
				this.resumePercent = pct;
				this.cdr.detectChanges();
			},
			error: ()    => {
				this.resumePercent = 0;
			}
		});
	}

	goToSimilarMovie(id: number): void {
        this.router.navigate(['/movie', id]).then(() => {
            window.location.reload();
        });
    }

	loadUserProfile(): void {
		this.userService.getUserProfile().subscribe({
			next: (data) => {
				this.user = {
				...data,
				profilePictureUrl: data.avatar 
				};
				this.cdr.detectChanges();
			}
		});
	}

	goToProfile(): void { this.router.navigate(['/profile']); }

	ngOnDestroy(): void {
		this.saveCurrentProgress();
		this.progressSub?.unsubscribe();
	}

	@HostListener('window:beforeunload')
	onBeforeUnload(): void {
		this.saveCurrentProgress();
	}

	goBack(): void {
		this.router.navigate(['/home']);
	}

	// ═══════════════════════════════════════════════════════════════════════════
	//  Format helpers
	// ═══════════════════════════════════════════════════════════════════════════

	formatRuntime(minutes: number): string {
		const h = Math.floor(minutes / 60);
		const m = minutes % 60;
		return h > 0 ? `${h}h ${m}m` : `${m}m`;
	}

	formatMoney(amount: number): string {
		if (!amount) return 'N/A';
		return new Intl.NumberFormat('en-US', {
			style: 'currency', currency: 'USD', notation: 'compact'
		}).format(amount);
	}

	formatTime(seconds: number): string {
		if (!seconds || isNaN(seconds) || !isFinite(seconds))
			return '0:00';
		const h = Math.floor(seconds / 3600);
		const m = Math.floor((seconds % 3600) / 60);
		const s = Math.floor(seconds % 60);
		return h > 0
			? `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
			: `${m}:${s.toString().padStart(2, '0')}`;
	}

	// ═══════════════════════════════════════════════════════════════════════════
	//  Stream URL builder
	// ═══════════════════════════════════════════════════════════════════════════

	private buildStreamUrl(startSeconds: number): string {
		return `/api/movies/${this.movieId}/transcode?startSeconds=${startSeconds}&t=${Date.now()}`;
	}

	adjustSubtitleDelay(seconds: number): void {
        this.subtitleDelay += seconds;
        this.subtitleDelay = Math.round(this.subtitleDelay * 10) / 10; 
        this.applySubtitleTrack();
    }

	// ═══════════════════════════════════════════════════════════════════════════
	//  Watch / download flow
	// ═══════════════════════════════════════════════════════════════════════════

	showPlayer(resume = false): void {
		if (!this.movie) return;

		if (this.playerVisible) {
			this.scrollToPlayer();
			return;
		}

		this.isStreamReady = false;

		if (this.downloadStatus === 'downloading') {
			this._pendingResume = resume && this.resumePercent > 0;
			if (!this._pendingResume) {
				this.playerVisible = true;
				setTimeout(() => this.scrollToPlayer(), 100);
			}
			return;
		}

		if (this.downloadStatus === 'loading')
			return;

		this.downloadStatus = 'loading';
		this.cdr.detectChanges();

		const { id, title, release_date } = this.movie;
		const year = release_date?.split('-')[0] ?? '';

		this.movieService.watchMovie(id, title, year).subscribe({
			next: (res) => {
				if (res.status === 'already_downloaded') {
					this.downloadStatus = 'idle';
					this.downloadProgress = 100;
					this.applyResumeAndShow(resume);
					return;
				}

				this.downloadStatus  = 'downloading';
				this.downloadProgress = 0;
				this._pendingResume  = resume && this.resumePercent > 0;
				this.cdr.detectChanges();

				this.progressSub = this.movieService
					.getDownloadProgress(id)
					.subscribe({
						next:  (pct) => {
							this.downloadProgress = pct;
							this.checkResumeReady(resume);
							this.cdr.detectChanges();
						},
						error: ()    => {
							this.downloadStatus = 'error';
							this.downloadMessage = 'Error tracking download.';
							this.cdr.detectChanges();
						}
					});
				},
			error: () => {
				this.downloadStatus  = 'error';
				this.downloadMessage = 'No torrent found for this movie.';
				this.cdr.detectChanges();
			}
		});
	}

	private checkResumeReady(resume: boolean): void {
		const needed = Math.min(
			resume && this.resumePercent > 0 ? this.resumePercent + 5 : 5,
			100
		);

		this.isReadyToResume = this.downloadProgress >= needed;

		if (!this.playerVisible && this.isReadyToResume) {
			this.applyResumeAndShow(resume);
		}
	}

	private applyResumeAndShow(resume: boolean): void {
		const seekSeconds = (resume && this.resumePercent > 0 && this.totalDuration > 0)
			? Math.floor((this.resumePercent / 100) * this.totalDuration)
			: 0;

		this.currentOffset = seekSeconds;
		this.playerVisible = true;
		this.isStreamReady = false;

		const originalLang = this.movie?.original_language ?? 'en';
		if (originalLang === 'en') {
			this.subtitleLang = 'en';
			this.subtitleUrl  = `/api/movies/${this.movieId}/subtitles?lang=en&imdbId=${(this.movie as any).imdb_id}`;
			this.showLangSelector = false;
		} else {
			this.showLangSelector = true;
		}

		this.cdr.detectChanges();

		setTimeout(() => {
			this.scrollToPlayer();
			this.waitForStreamReady(seekSeconds);
		}, 200);
	}

	loadSubtitle(lang: string): void {
        if (!this.movie) return;
        const imdbId = (this.movie as any).imdb_id ?? '';
        if (!imdbId) return;
        
        this.subtitleLang = lang;
        this.subtitleUrl  = `/api/movies/${this.movieId}/subtitles?lang=${lang}&imdbId=${imdbId}`;
        this.showLangSelector = false;

        this.originalVttText = null; 

        this.applySubtitleTrack();
        this.cdr.detectChanges();
    }

	private applySubtitleTrack(): void {
		const v = this.videoRef?.nativeElement;
		if (!v || !this.subtitleUrl) return;

		const myId = ++this.subtitleRequestId;

		Array.from(v.querySelectorAll('track')).forEach(t => t.remove());
		for (let i = 0; i < v.textTracks.length; i++) {
			v.textTracks[i].mode = 'hidden';
		}

		if (this.currentOffset > 0) {
			fetch(this.subtitleUrl)
				.then(r => r.text())
				.then(vttText => {
					if (myId !== this.subtitleRequestId) return;
					const shifted = this.shiftVttTimes(vttText, this.currentOffset);
					const url     = URL.createObjectURL(new Blob([shifted], { type: 'text/vtt' }));
					this.attachTrack(url, v);
				})
		} else {
			this.attachTrack(this.subtitleUrl, v);
		}
	}

	private attachTrack(src: string, v: HTMLVideoElement): void {
		const track    = document.createElement('track');
		track.kind     = 'subtitles';
		track.src      = src;
		track.srclang  = this.subtitleLang;
		track.label    = this.availableLangs.find(l => l.code === this.subtitleLang)?.label ?? this.subtitleLang;
		track.default  = true;
		v.crossOrigin  = 'anonymous';
		v.appendChild(track);

		const activate = () => {
			for (let i = 0; i < v.textTracks.length; i++) {
				v.textTracks[i].mode = v.textTracks[i].language === this.subtitleLang
					? 'showing' : 'hidden';
			}
		};
		track.addEventListener('load', activate);
		setTimeout(activate, 250);
	}


	private addTrackToVideo(srcUrl: string, videoElement: HTMLVideoElement): void {
        const track = document.createElement('track');
        track.kind = 'subtitles';
        track.src = srcUrl;
        track.srclang = this.subtitleLang;
        track.label = this.availableLangs.find(l => l.code === this.subtitleLang)?.label ?? this.subtitleLang;
        track.default = true;

        videoElement.appendChild(track);

        const forceActivate = () => {
            const tracks = videoElement.textTracks;
            for (let i = 0; i < tracks.length; i++) {
                if (tracks[i].language === this.subtitleLang || tracks[i].label === track.label) {
                    tracks[i].mode = 'showing';
                } else {
                    tracks[i].mode = 'hidden';
                }
            }
        };

        track.onload = forceActivate;
        setTimeout(forceActivate, 50);
        setTimeout(forceActivate, 250);
    }

	private shiftAndLoadVtt(
		vttUrl: string,
		offsetSeconds: number,
		videoElement: HTMLVideoElement,
		requestId: number
	): Promise<void> {

		return fetch(vttUrl)
			.then(r => {return r.text(); })
			.then(vttText => {
				if (requestId !== this.subtitleRequestId) {
					return;
				}
				const shiftedVtt = this.shiftVttTimes(vttText, offsetSeconds);
				const blob    = new Blob([shiftedVtt], { type: 'text/vtt' });
				const blobUrl = URL.createObjectURL(blob);
				this.addTrackToVideo(blobUrl, videoElement);
			})
	}

    private shiftVttTimes(vtt: string, offsetSeconds: number): string {
        const blocks = vtt.split(/\r?\n\s*\r?\n/);
        const newBlocks: string[] = [];

        if (blocks.length > 0 && blocks[0].includes('WEBVTT')) {
            newBlocks.push(blocks[0]);
        }

        const timeRegex = /(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})/;
        let matchCount = 0;
        let deletedCount = 0;

        for (let i = 1; i < blocks.length; i++) {
            const block = blocks[i];
            const match = block.match(timeRegex);

            if (match) {
                const [_, h1, m1, s1, ms1, h2, m2, s2, ms2] = match;

                const getSecs = (h: string, m: string, s: string, ms: string) => 
                    parseInt(h) * 3600 + parseInt(m) * 60 + parseInt(s) + parseInt(ms) / 1000;

                let startSecs = getSecs(h1, m1, s1, ms1) - offsetSeconds + this.subtitleDelay;
                let endSecs = getSecs(h2, m2, s2, ms2) - offsetSeconds + this.subtitleDelay;

                if (endSecs <= 0) {
                    deletedCount++;
                    continue; 
                }

                if (startSecs < 0) startSecs = 0;

                const formatTime = (secs: number) => {
                    let h = Math.floor(secs / 3600);
                    let m = Math.floor((secs % 3600) / 60);
                    let s = Math.floor(secs % 60);
                    let ms = Math.round((secs - Math.floor(secs)) * 1000);
                    if (ms >= 1000) {
                        s += Math.floor(ms / 1000);
                        ms = ms % 1000;
                    }
                    if (s >= 60) {
                        m += Math.floor(s / 60);
                        s = s % 60;
                    }
                    if (m >= 60) {
                        h += Math.floor(m / 60);
                        m = m % 60;
                    }

                    const pad = (num: number, size: number) => num.toString().padStart(size, '0');
                    return `${pad(h, 2)}:${pad(m, 2)}:${pad(s, 2)}.${pad(ms, 3)}`;
                };

                const newTimeLine = `${formatTime(startSecs)} --> ${formatTime(endSecs)}`;
                const newBlock = block.replace(timeRegex, newTimeLine);
                newBlocks.push(newBlock);
                matchCount++;

            } else {
                if (block.trim()) newBlocks.push(block);
            }
        }
        return newBlocks.join('\n\n');
    }

	private retryWait(seekSeconds: number, attempts: number, max: number, ms: number): void {
		if (attempts >= max) {
			this.downloadStatus  = 'error';
			this.downloadMessage = 'Stream took too long to start.';
			this.cdr.detectChanges();
			return;
		}
		setTimeout(() => this.waitForStreamReady(seekSeconds, attempts + 1), ms);
	}

	private waitForStreamReady(seekSeconds: number, attempts = 0): void {
		const url = this.buildStreamUrl(seekSeconds);
		const MAX_ATTEMPTS = 20;
		const RETRY_MS = 1500;

		fetch(url, { method: 'GET', headers: { Range: 'bytes=0-1023' } })
			.then(res => {
				if ((res.status === 200 || res.status === 206) && res.body) {
					return res.body.getReader().read().then(({ value }) => {
						if (value && value.length > 0) {
							this.loadAndPlay(url);
						} else {
							this.retryWait(seekSeconds, attempts, MAX_ATTEMPTS, RETRY_MS);
						}
					});
				}
				this.retryWait(seekSeconds, attempts, MAX_ATTEMPTS, RETRY_MS);
				return Promise.resolve();
			})
			.catch(() => this.retryWait(seekSeconds, attempts, MAX_ATTEMPTS, RETRY_MS));
	}

	// ═══════════════════════════════════════════════════════════════════════════
	//  Video element helpers
	// ═══════════════════════════════════════════════════════════════════════════
	private loadAndPlay(url: string): void {
		const v = this.videoRef?.nativeElement;
		if (!v) return;

		let started = false;

		v.pause();
		v.removeAttribute('src');
		v.load();

		const startPlay = () => {
			if (started) return;
			started = true;
			clearTimeout(fallback);
			v.removeEventListener('canplay', startPlay);

			v.play()
				.then(() => {
					this.isPlaying     = true;
					this.isStreamReady = true;
					this.cdr.detectChanges();
					if (this.subtitleUrl) {
						this.applySubtitleTrack();
					}
				})
				.catch(err => {
					this.isStreamReady = true;
					this.isPlaying     = false;
					this.cdr.detectChanges();
					if (this.subtitleUrl) {
						this.applySubtitleTrack();
					}
				});
		};

		const fallback = setTimeout(startPlay, 10000);
		v.addEventListener('canplay', startPlay);
		v.src = url;
		v.load();
	}

	private scrollToPlayer(): void {
		document.querySelector('.player-section')?.scrollIntoView({ behavior: 'smooth' });
	}

	private get video(): HTMLVideoElement {
		return this.videoRef.nativeElement;
	}

	private get effectiveDuration(): number {
		return this.totalDuration || this.video.duration || 0;
	}

	// ═══════════════════════════════════════════════════════════════════════════
	//  Player event handlers
	// ═══════════════════════════════════════════════════════════════════════════

	onStreamDataLoaded(): void {
		this.isStreamReady = true;
		this.cdr.detectChanges();
	}

	onMetadata(): void {
		this.durationStr = this.formatTime(this.effectiveDuration);
		if (this.video.paused) {
			this.video.play();
		}
		this.isPlaying = true;
		this.cdr.detectChanges();
	}

	onTimeUpdate(): void {
		const now = Date.now();
		if (now - this.lastTimeUpdate < 500)
			return;
		this.lastTimeUpdate = now;

		const duration = this.effectiveDuration;

		if (duration && isFinite(duration) && duration > 0) {
			const absolute    = this.currentOffset + this.video.currentTime;
			this.playedPercent  = Math.min((absolute / duration) * 100, 100);
			this.currentTimeStr = this.formatTime(absolute);
			this.cdr.detectChanges();
		}
	}

	onPause(): void  {
		this.saveCurrentProgress();
	}

	onEnded(): void  {
		this.saveCurrentProgress();
	}

	// ═══════════════════════════════════════════════════════════════════════════
	//  Player controls
	// ═══════════════════════════════════════════════════════════════════════════

	togglePlay(): void {
		const v = this.video;
		if (v.paused) {
			v.play();
			this.isPlaying = true;
		}
		else {
			v.pause();
			this.isPlaying = false;
		}
		this.cdr.detectChanges();
	}

	// ═══════════════════════════════════════════════════════════════════════════
    //  Atajos de Teclado (Bonus)
    // ═══════════════════════════════════════════════════════════════════════════

    @HostListener('window:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent): void {

        if (!this.playerVisible || !this.isStreamReady) return;

        const target = event.target as HTMLElement;
        if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return;

        switch (event.key.toLowerCase()) {
            case ' ':
                event.preventDefault();
                this.togglePlay();
                break;
            case 'f':
                event.preventDefault();
                this.toggleFullscreen();
                break;
            case 'm':
                event.preventDefault();
                this.toggleMute();
                break;
			case 'p':
                event.preventDefault();
                this.togglePiP();
                break;
            case 'arrowright':
                event.preventDefault();
                this.seekRelative(10);
                break;
            case 'arrowleft':
                event.preventDefault();
                this.seekRelative(-10);
                break;
        }
    }

    toggleMute(): void {
        const v = this.video;
        if (!v) return;

        if (v.volume > 0) {
            this.previousVolume = v.volume;
            v.volume = 0;
            this.volume = 0;
        } else {
            v.volume = this.previousVolume || 1;
            this.volume = this.previousVolume || 1;
        }
        this.cdr.detectChanges();
    }

    seekRelative(seconds: number): void {
        const duration = this.totalDuration;
        if (!duration || duration <= 0 || !this.video)
			return;

        let newAbsolute = Math.floor(this.currentOffset + this.video.currentTime + seconds);
        
        if (newAbsolute < 0)
			newAbsolute = 0;

        if (newAbsolute > duration)
			newAbsolute = duration;

        const targetPercent = (newAbsolute / duration) * 100;
        const isFullyDownloaded = this.downloadProgress >= 100 || this.downloadStatus === 'idle';
        const maxSeekable = isFullyDownloaded ? 100 : Math.max(0, this.downloadProgress);

        if (targetPercent > maxSeekable)
			return; 

        this.reconnectAttempts = 0;
        clearTimeout(this.seekDebounce);
        this.saveCurrentProgress();

        this.seekDebounce = setTimeout(() => {
            this.isStreamReady = false;
            this.isPlaying     = false;
            this.currentOffset = newAbsolute;
            this.cdr.detectChanges();
            this.waitForStreamReady(newAbsolute);
        }, 300);
    }

	togglePiP(): void {
        const v = this.videoRef?.nativeElement as any;
        if (!v)
			return;

        if (document.pictureInPictureElement) {
            document.exitPictureInPicture();
        } 
        else if (typeof v.requestPictureInPicture === 'function') {
            v.requestPictureInPicture();
        } 
        else if (typeof v.webkitSetPresentationMode === 'function') {
            v.webkitSetPresentationMode(v.webkitPresentationMode === "picture-in-picture" ? "inline" : "picture-in-picture");
        } 
   
    }

	seekTo(event: MouseEvent): void {
		const bar          = event.currentTarget as HTMLElement;
		const clickPercent = (event.offsetX / bar.clientWidth) * 100;
		
		const isFullyDownloaded = this.downloadProgress >= 100 
			|| this.downloadStatus === 'idle';
		
		const maxSeekable = isFullyDownloaded
			? 100
			: Math.max(0, this.downloadProgress );

		if (clickPercent > maxSeekable) return;

		const duration = this.totalDuration;
		if (!duration || duration <= 0) return;

		const seekSeconds = Math.floor((clickPercent / 100) * duration);

		this.reconnectAttempts = 0;
		clearTimeout(this.seekDebounce);
		this.saveCurrentProgress();

		this.seekDebounce = setTimeout(() => {
			this.isStreamReady = false;
			this.isPlaying     = false;
			this.currentOffset = seekSeconds;
			this.cdr.detectChanges();
			this.waitForStreamReady(seekSeconds);
		}, 300);
	}

	onProgressHover(event: MouseEvent): void {
		const bar          = event.currentTarget as HTMLElement;
		this.hoverPercent  = (event.offsetX / bar.clientWidth) * 100;
		const maxSeekable  = this.downloadProgress >= 100 ? 100 : this.downloadProgress;
		this.canSeekHover  = this.hoverPercent <= maxSeekable;
		this.cdr.detectChanges();
	}

	onProgressLeave(): void {
		this.hoverPercent = 0;
		this.canSeekHover = false;
		this.cdr.detectChanges();
	}

	setVolume(event: Event): void {
		this.video.volume = parseFloat((event.target as HTMLInputElement).value);
		this.volume = this.video.volume;
	}

	toggleFullscreen(): void {
		const container = this.playerContainerRef.nativeElement;
		if (!document.fullscreenElement) {
			container.requestFullscreen();
		} else {
			document.exitFullscreen();
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════
	//  Progress persistence
	// ═══════════════════════════════════════════════════════════════════════════

	private saveCurrentProgress(forceComplete = false): void {
		if (!this.movie || !this.videoRef?.nativeElement)
			return;

		const duration = this.effectiveDuration;
		if (!duration || duration <= 0)
			return;

		const absolute = this.currentOffset + this.video.currentTime;
		const percent  = Math.min(Math.max(forceComplete ? 100 : (absolute / duration) * 100, 0), 100);

		if (percent <= 1 && !forceComplete)
			return;

		const { id, title, release_date, poster_path } = this.movie;
		const year   = release_date?.split('-')[0] ?? '';
		const poster = poster_path ?? '';

		this.historyService.saveProgress(id, title, year, poster, percent).subscribe();
	}


	private loadFavoriteState(): void {
		this.userService.getFavoriteMovies().subscribe({
			next: (favorites) => {
				this.isFavorite = favorites.some(fav => fav.movieId === this.movieId);
				this.cdr.detectChanges();
			}
		})
	}


	toggleFavorite(): void {
        if (this.isTogglingFavorite)
			return; 
	
        this.isTogglingFavorite = true;

        if (this.isFavorite) {
			this.userService.removeFavoriteMovie(this.movieId).subscribe({
                next: () => {
                    this.isFavorite = false; 
                    this.isTogglingFavorite = false;
                    this.cdr.detectChanges();
                },
                error: (err) => {
                    if (err.error && err.error.error) {
                        this.isFavorite = false;
                    }
                    this.isTogglingFavorite = false;
                    this.cdr.detectChanges();
                }
            });
        } else {
            this.userService.addFavoriteMovie(this.movieId).subscribe({
                next: () => {
                    this.isFavorite = true;
                    this.isTogglingFavorite = false;
                    this.cdr.detectChanges();
                },
                error: (err) => {
                    if (err.status === 409 && err.error && err.error.error) {
                        this.isFavorite = true;
                    }
                    this.isTogglingFavorite = false;
                    this.cdr.detectChanges();
                }
            });
        }
    }

	private loadComments(): void {
		this.isLoadingComments = true;
		this.commentService.getComments(this.movieId).subscribe({
			next: (data) => {
				this.comments = data;
				this.isLoadingComments = false;
				this.cdr.detectChanges();
			},
			error: () => {
				this.isLoadingComments = false;
				this.cdr.detectChanges();
			}
		});
	}

	submitComment(): void {
		const text = this.newCommentText.trim();
		if (!text || this.isSubmittingComment)
			return;

		this.isSubmittingComment = true;
		this.commentError = '';

		this.commentService.postComment(this.movieId, text).subscribe({
			next: (comment) => {
				this.comments = [comment, ...this.comments];
				this.newCommentText = '';
				this.isSubmittingComment = false;
				this.cdr.detectChanges();
			},
			error: (err) => {
				this.commentError = err?.error?.message ?? 'Error posting comment.';
				this.isSubmittingComment = false;
				this.cdr.detectChanges();
			}
		});
	}

	getInitials(firstName: string, lastName: string): string {
		return `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase();
	}

	goToUserProfile(username: string): void {
		if (!username) return;
		this.router.navigate(['/user', username]);
	}


	logout(): void {
		document.cookie = 'auth_token=; Max-Age=0; path=/';
		this.router.navigate(['/landing']);
	}

}